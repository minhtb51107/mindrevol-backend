package com.mindrevol.backend.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
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

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // Xác định giao thức
        String protocol = sslEnabled ? "rediss" : "redis";
        String address = protocol + "://" + redisHost + ":" + redisPort;

        // Cấu hình Single Server
        SingleServerConfig serverConfig = config.useSingleServer()
              .setAddress(address)
              
              // --- [TIMEOUT & MẠNG] ---
              .setConnectTimeout(30000)
              .setTimeout(30000)
              .setRetryAttempts(3)
              .setRetryInterval(1500)
              
              // [QUAN TRỌNG] Ping mỗi 60s để giữ kết nối mà không tốn Quota Upstash
              .setPingConnectionInterval(60000) 
              .setKeepAlive(true)

              // --- [CẤU HÌNH CÂN BẰNG GIỮA HIỆU NĂNG VÀ RAM] ---
              // [FIX] Tăng từ 2 lên 8. 
              // Lý do: Cần đủ kết nối cho các Worker chạy nền (BLPOP) + Request từ người dùng.
              // 8 kết nối vẫn an toàn cho RAM 512MB nếu đã giới hạn Heap Size.
              .setConnectionPoolSize(8)
              
              // Giữ tối thiểu 2 kết nối sẵn sàng (thay vì 1) để phản hồi nhanh hơn
              .setConnectionMinimumIdleSize(2)
              
              .setDnsMonitoringInterval(60000);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            serverConfig.setPassword(redisPassword);
        }

        return Redisson.create(config);
    }
}