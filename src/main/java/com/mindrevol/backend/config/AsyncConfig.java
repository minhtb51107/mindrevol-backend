package com.mindrevol.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync 
public class AsyncConfig {

    // 1. Executor chung cho các tác vụ nhẹ (Notification, Event nội bộ)
    // Đặc điểm: Xử lý nhanh, cần phản hồi tức thì, số lượng nhiều.
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);      // Tăng lên 5
        executor.setMaxPoolSize(20);      // Tăng lên 20 để chịu tải cao
        executor.setQueueCapacity(500);   
        executor.setThreadNamePrefix("Async-General-");
        executor.initialize();
        return executor;
    }

    // 2. Executor riêng cho xử lý ẢNH (Nặng CPU & RAM)
    // Đặc điểm: Tốn tài nguyên, nên giới hạn số luồng chạy song song để tránh OOM (Hết RAM).
    @Bean(name = "imageTaskExecutor")
    public Executor imageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      // Chỉ cho phép 2 ảnh xử lý cùng lúc
        executor.setMaxPoolSize(4);       // Tối đa 4
        executor.setQueueCapacity(100);   // Hàng đợi ngắn hơn, nếu đầy thì user phải chờ
        executor.setThreadNamePrefix("Async-Image-");
        executor.initialize();
        return executor;
    }
}