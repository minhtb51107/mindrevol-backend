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
              
              // [QUAN TRỌNG - SỬA LỖI TỐN QUOTA]
              // Tăng thời gian ping lên 60 giây (Thay vì 10s/20s)
              // Tính toán: 1 phút 1 lần * 60 * 24 * 30 = 43,200 lệnh/tháng/kết nối (An toàn)
              .setPingConnectionInterval(60000) 
              .setKeepAlive(true)

              // [QUAN TRỌNG - TIẾT KIỆM RAM & QUOTA]
              // Chỉ cho phép tối đa 2 kết nối (Đủ cho app nhỏ, tiết kiệm lệnh ping)
              .setConnectionPoolSize(2)
              
              // Chỉ giữ 1 kết nối khi rảnh
              .setConnectionMinimumIdleSize(1)
              
              // Kiểm tra DNS ít hơn (mỗi 1 phút) để đỡ tốn CPU
              .setDnsMonitoringInterval(60000);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            serverConfig.setPassword(redisPassword);
        }

        return Redisson.create(config);
    }
}