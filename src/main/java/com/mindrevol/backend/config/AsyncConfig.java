package com.mindrevol.backend.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync 
public class AsyncConfig {

    // 1. Executor chung cho các tác vụ nhẹ (Notification, Event nội bộ)
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);      
        executor.setMaxPoolSize(20);      
        executor.setQueueCapacity(500);   
        executor.setThreadNamePrefix("Async-General-");
        
        // [QUAN TRỌNG] Copy RequestID sang luồng mới
        executor.setTaskDecorator(new MdcTaskDecorator());
        
        executor.initialize();
        return executor;
    }

    // 2. Executor riêng cho xử lý ẢNH (Nặng CPU & RAM)
    @Bean(name = "imageTaskExecutor")
    public Executor imageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      
        executor.setMaxPoolSize(4);       
        executor.setQueueCapacity(100);   
        executor.setThreadNamePrefix("Async-Image-");
        
        // [QUAN TRỌNG] Copy RequestID sang luồng mới
        executor.setTaskDecorator(new MdcTaskDecorator());
        
        executor.initialize();
        return executor;
    }

    /**
     * Class phụ trợ giúp copy ngữ cảnh (MDC) từ luồng chính sang luồng Async.
     * Giúp log ở thread con vẫn giữ được requestId, userId...
     */
    public static class MdcTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // Lấy map MDC của luồng hiện tại (Luồng cha)
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    // Gán vào luồng mới (Luồng con)
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    // Dọn dẹp sau khi chạy xong để tránh rác bộ nhớ
                    MDC.clear();
                }
            };
        }
    }
}