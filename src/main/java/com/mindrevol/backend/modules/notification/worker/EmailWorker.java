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

    // Sử dụng ThreadPool cố định 5 luồng để xử lý email song song
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

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

        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (redissonClient.isShutdown()) break;

                // [FIX QUAN TRỌNG] Thay take() bằng poll(5 giây)
                // Lý do: take() sẽ chờ mãi mãi. Nếu Upstash ngắt kết nối, thread này sẽ bị treo vĩnh viễn.
                // poll() sẽ nhả ra sau 5s để thread có cơ hội kiểm tra lại trạng thái mạng/shutdown.
                EmailTask task = queue.poll(5, TimeUnit.SECONDS);

                if (task != null) {
                    handleTask(task, queue);
                }
                // Nếu task == null (hết 5s không có mail), vòng lặp chạy lại, thread vẫn sống khỏe.

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (RedissonShutdownException e) {
                break;
            } catch (Exception e) {
                // Nếu gặp lỗi mạng (Redis connection closed), log nhẹ và đợi xíu rồi thử lại
                if (e.getCause() instanceof RedissonShutdownException) break;
                
                log.error("Error processing email queue loop: {}", e.getMessage());
                try { Thread.sleep(3000); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); break; }
            }
        }
    }

    private void handleTask(EmailTask task, RBlockingQueue<EmailTask> queue) {
        try {
            // Gọi service gửi mail (SMTP)
            emailService.sendEmail(task.getToEmail(), task.getSubject(), task.getContent());
            log.info("✅ Email sent successfully to {}", task.getToEmail());
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}", task.getToEmail(), e);
            retryTask(task, queue);
        }
    }

    private void retryTask(EmailTask task, RBlockingQueue<EmailTask> queue) {
        if (task.getRetryCount() < 3) {
            task.setRetryCount(task.getRetryCount() + 1);
            log.warn("🔄 Retrying task for {} (Attempt {})", task.getToEmail(), task.getRetryCount());
            if (!redissonClient.isShutdown()) {
                // Đẩy lại vào cuối hàng đợi
                queue.add(task);
            }
        } else {
            log.error("💀 Email task for {} failed after 3 attempts. Discarding.", task.getToEmail());
        }
    }
}