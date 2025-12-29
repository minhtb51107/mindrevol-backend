package com.mindrevol.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean; // <--- Thêm import này
import org.springframework.web.client.RestTemplate; // <--- Thêm import này
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableRetry
public class MindrevolBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindrevolBackendApplication.class, args);
    }

    // --- THÊM ĐOẠN NÀY ---
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    // ---------------------
}