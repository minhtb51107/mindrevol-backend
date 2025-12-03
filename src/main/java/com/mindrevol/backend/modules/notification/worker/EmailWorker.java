package com.mindrevol.backend.modules.notification.worker;

import com.mindrevol.backend.modules.notification.dto.EmailTask;
import com.mindrevol.backend.modules.notification.service.EmailService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailWorker {

    private final RedissonClient redissonClient;
    private final EmailService emailService;
    
    private static final String EMAIL_QUEUE_NAME = "email_queue";
    
    // --- THAY ĐỔI: Dùng ThreadPool cố định 5 luồng để gửi mail song song ---
    private final ExecutorService executorService = Executors.newFixedThreadPool(5); 
    // ----------------------------------------------------------------------

    @PostConstruct
    public void startWorker() {
        // Khởi động 5 consumer chạy song song, cùng lắng nghe 1 queue
        for (int i = 0; i < 5; i++) {
            executorService.submit(this::processQueue);
        }
    }

    @PreDestroy
    public void stopWorker() {
        log.info("Stopping Email Worker...");
        executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Email Worker did not terminate gracefully");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processQueue() {
        if (redissonClient.isShutdown() || redissonClient.isShuttingDown()) {
            return;
        }
        
        RBlockingQueue<EmailTask> queue = redissonClient.getBlockingQueue(EMAIL_QUEUE_NAME);
        // Lưu ý: Không cần log "started listening" ở đây nữa vì sẽ bị spam 5 lần

        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (redissonClient.isShutdown()) break;
                
                // RBlockingQueue an toàn với đa luồng (Thread-safe)
                EmailTask task = queue.take(); 
                handleTask(task, queue);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (RedissonShutdownException e) {
                break;
            } catch (Exception e) {
                if (e.getCause() instanceof RedissonShutdownException) break;
                log.error("Error processing email queue", e);
                try { Thread.sleep(1000); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); break; }
            }
        }
    }

    private void handleTask(EmailTask task, RBlockingQueue<EmailTask> queue) {
        try {
            // [Giữ nguyên logic gửi mail]
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
            if (!redissonClient.isShutdown()) {
                queue.add(task);
            }
        } else {
            log.error("Email task for {} failed after 3 attempts. Discarding.", task.getToEmail());
        }
    }
}