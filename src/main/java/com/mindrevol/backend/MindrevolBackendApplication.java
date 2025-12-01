package com.mindrevol.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling; // <--- Import này

@SpringBootApplication
//@EnableJpaAuditing
@EnableAsync
@EnableScheduling // <--- THÊM DÒNG NÀY
public class MindrevolBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindrevolBackendApplication.class, args);
    }

}