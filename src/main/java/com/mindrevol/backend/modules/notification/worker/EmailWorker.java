package com.mindrevol.backend.modules.notification.worker;

import com.mindrevol.backend.modules.notification.dto.EmailTask;
import com.mindrevol.backend.modules.notification.service.EmailService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy; // 1. Thêm import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException; // 2. Thêm import
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit; // 3. Thêm import

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailWorker {

    private final RedissonClient redissonClient;
    private final EmailService emailService;
    
    private static final String EMAIL_QUEUE_NAME = "email_queue";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void startWorker() {
        executorService.submit(this::processQueue);
    }

    // 4. Thêm method xử lý khi ứng dụng tắt
    @PreDestroy
    public void stopWorker() {
        log.info("Stopping Email Worker...");
        executorService.shutdownNow(); // Gửi tín hiệu interrupt tới luồng đang chạy
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Email Worker did not terminate gracefully");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processQueue() {
        // Kiểm tra Redisson có shutdown không trước khi lấy queue
        if (redissonClient.isShutdown() || redissonClient.isShuttingDown()) {
            return;
        }
        
        RBlockingQueue<EmailTask> queue = redissonClient.getBlockingQueue(EMAIL_QUEUE_NAME);
        log.info("Email Worker started listening on queue: {}", EMAIL_QUEUE_NAME);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Kiểm tra lại trong vòng lặp
                if (redissonClient.isShutdown()) {
                    break;
                }
                
                EmailTask task = queue.take(); // Chặn luồng cho đến khi có task
                handleTask(task, queue);
            } catch (InterruptedException e) {
                log.info("Email Worker interrupted, stopping...");
                Thread.currentThread().interrupt();
                break;
            } catch (RedissonShutdownException e) {
                // 5. Bắt lỗi Redisson shutdown để thoát vòng lặp
                log.info("Redisson is shutting down, stopping Email Worker loop.");
                break;
            } catch (Exception e) {
                // Kiểm tra nếu lỗi là do Redisson shutdown bị wrap bên trong
                if (e.getCause() instanceof RedissonShutdownException) {
                    log.info("Redisson is shutting down (cause), stopping Email Worker loop.");
                    break;
                }
                log.error("Error processing email queue", e);
                // Thêm sleep nhỏ để tránh spam log nếu có lỗi liên tục khác
                try { Thread.sleep(1000); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); break; }
            }
        }
    }

    private void handleTask(EmailTask task, RBlockingQueue<EmailTask> queue) {
        try {
            log.info("Processing email for: {}", task.getToEmail());
            emailService.sendEmail(task.getToEmail(), task.getSubject(), task.getContent());
            log.info("Email sent successfully to {}", task.getToEmail());
        } catch (Exception e) {
            log.error("Failed to send email to {}", task.getToEmail(), e);
            retryTask(task, queue);
        }
    }

    private void retryTask(EmailTask task, RBlockingQueue<EmailTask> queue) {
        if (task.getRetryCount() < 3) {
            task.setRetryCount(task.getRetryCount() + 1);
            log.warn("Retrying task for {} (Attempt {})", task.getToEmail(), task.getRetryCount());
            
            // Cần check shutdown trước khi add lại để tránh lỗi
            if (!redissonClient.isShutdown()) {
                queue.add(task);
            }
        } else {
            log.error("Email task for {} failed after 3 attempts. Discarding.", task.getToEmail());
        }
    }
}