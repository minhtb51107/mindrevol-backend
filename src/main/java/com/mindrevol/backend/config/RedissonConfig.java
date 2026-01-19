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

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 1. Xác định giao thức (rediss:// cho SSL, redis:// cho thường)
        String protocol = sslEnabled ? "rediss" : "redis";
        String address = protocol + "://" + redisHost + ":" + redisPort;

        // 2. Cấu hình Single Server
        SingleServerConfig serverConfig = config.useSingleServer()
              .setAddress(address)

              // --- [TIMEOUT & MẠNG] ---
              .setConnectTimeout(30000)
              .setTimeout(30000)
              .setRetryAttempts(3)
              .setRetryInterval(1500)

              // [QUAN TRỌNG] Ping mỗi 30s (An toàn hơn 60s) để giữ kết nối không bị Upstash cắt
              .setPingConnectionInterval(30000)
              .setKeepAlive(true)

              // --- [CẤU HÌNH POOL] ---
              // Pool size 8 là hợp lý: Đủ cho 5 luồng Worker + vài Request từ API
              // Với RAM 512MB, con số này vẫn nằm trong ngưỡng an toàn
              .setConnectionPoolSize(8)
              // Giữ tối thiểu 2 kết nối sẵn sàng để phản hồi nhanh
              .setConnectionMinimumIdleSize(2)

              // [QUAN TRỌNG] Check IP mới mỗi 5 giây (Upstash đổi IP rất thường xuyên)
              .setDnsMonitoringInterval(5000);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            serverConfig.setPassword(redisPassword);
        }

        return Redisson.create(config);
    }
}