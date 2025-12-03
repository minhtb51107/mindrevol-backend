package com.mindrevol.backend.common.service;

import com.mindrevol.backend.modules.journey.recap.dto.VideoTask;
import com.mindrevol.backend.modules.notification.dto.EmailTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncTaskProducer {

    private final RedissonClient redissonClient;
    private static final String EMAIL_QUEUE_NAME = "email_queue";
    private static final String VIDEO_QUEUE_NAME = "video_render_queue"; // Queue mới

    public void submitEmailTask(EmailTask task) {
        RBlockingQueue<EmailTask> queue = redissonClient.getBlockingQueue(EMAIL_QUEUE_NAME);
        queue.add(task);
        log.info("Task submitted to queue: Send email to {}", task.getToEmail());
    }

    // --- THÊM HÀM NÀY ---
    public void submitVideoTask(VideoTask task) {
        RBlockingQueue<VideoTask> queue = redissonClient.getBlockingQueue(VIDEO_QUEUE_NAME);
        queue.add(task);
        log.info("Task submitted to VIDEO queue for User {} Journey {}", task.getUserId(), task.getJourneyId());
    }
}