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

    // 1. Executor chung (Notification, v.v.)
    // [SURVIVAL MODE]: Giảm từ 5/20 xuống 2/4
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      // Giữ 2 luồng thường trực
        executor.setMaxPoolSize(4);       // Tối đa 4 luồng khi cao điểm
        executor.setQueueCapacity(100);   // Giảm hàng chờ xuống 100
        executor.setThreadNamePrefix("Async-Gen-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    // 2. Executor cho xử lý ẢNH
    // [SURVIVAL MODE]: Giảm tối đa, chỉ chạy 1 ảnh 1 lúc
    @Bean(name = "imageTaskExecutor")
    public Executor imageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);      // Chỉ 1 luồng xử lý ảnh
        executor.setMaxPoolSize(2);       // Tối đa 2 nếu quá gấp
        executor.setQueueCapacity(20);    
        executor.setThreadNamePrefix("Async-Img-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    public static class MdcTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        }
    }
}