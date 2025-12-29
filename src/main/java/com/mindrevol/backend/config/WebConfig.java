package com.mindrevol.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Áp dụng cho toàn bộ API
                .allowedOrigins(
                    "https://mindrevol.vercel.app", // Domain Vercel chính thức
                    "https://mindrevol-web.vercel.app", // Domain phụ nếu có
                    "http://localhost:5173" // Vẫn giữ localhost để Minh dev tiếp
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true) // Cho phép gửi cookie/token
                .maxAge(3600); // Cache cấu hình này trong 1 giờ
    }
}