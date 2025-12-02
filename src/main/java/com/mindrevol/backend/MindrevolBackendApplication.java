package com.mindrevol.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.retry.annotation.EnableRetry; // <--- THÊM IMPORT NÀY

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableRetry // <--- THÊM DÒNG NÀY ĐỂ BẬT CƠ CHẾ RETRY
public class MindrevolBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindrevolBackendApplication.class, args);
    }

}