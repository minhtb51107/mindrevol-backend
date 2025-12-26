package com.mindrevol.backend.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private String redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    // Lấy cờ SSL từ file properties (Prod = true, Dev = false)
    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 1. Chọn giao thức đúng (rediss cho Upstash, redis cho Local)
        String protocol = sslEnabled ? "rediss" : "redis";
        
        // 2. Cấu hình địa chỉ
        config.useSingleServer()
              .setAddress(protocol + "://" + redisHost + ":" + redisPort);

        // 3. Cấu hình mật khẩu (Quan trọng)
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }

        return Redisson.create(config);
    }
}