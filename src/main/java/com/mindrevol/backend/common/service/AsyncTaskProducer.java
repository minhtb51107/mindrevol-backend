package com.mindrevol.backend.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import com.mindrevol.backend.modules.notification.dto.EmailTask;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncTaskProducer {

    private final RedissonClient redissonClient;
    private static final String EMAIL_QUEUE_NAME = "email_queue";

    public void submitEmailTask(EmailTask task) {
        // Lấy hàng đợi từ Redis
        RBlockingQueue<EmailTask> queue = redissonClient.getBlockingQueue(EMAIL_QUEUE_NAME);
        
        // Đẩy việc vào (giống ghi sổ)
        queue.add(task);
        log.info("Task submitted to queue: Send email to {}", task.getToEmail());
    }
}