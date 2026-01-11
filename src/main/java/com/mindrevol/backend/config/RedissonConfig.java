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

        // Cấu hình Single Server (Upstash là single endpoint)
        SingleServerConfig serverConfig = config.useSingleServer()
              .setAddress(address)
              // [QUAN TRỌNG] Tăng Timeout để tránh lỗi mạng chập chờn
              .setConnectTimeout(30000)  // 30 giây (mặc định 10s)
              .setTimeout(30000)         // 30 giây chờ phản hồi lệnh
              .setRetryAttempts(3)       // Thử lại 3 lần nếu lỗi
              .setRetryInterval(1500)    // Chờ 1.5s giữa các lần thử
              
              // [QUAN TRỌNG] Giữ kết nối với Upstash để không bị "ngủ đông"
              .setPingConnectionInterval(10000) // Ping mỗi 10 giây
              .setKeepAlive(true);              // Bật TCP KeepAlive

        if (redisPassword != null && !redisPassword.isEmpty()) {
            serverConfig.setPassword(redisPassword);
        }

        return Redisson.create(config);
    }
}