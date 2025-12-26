package com.mindrevol.backend.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RateLimitingConfig {

    private final RedisProperties redisProperties;

    @Bean
    public RedisClient redisClient() {
        // 1. Kiểm tra xem profile hiện tại có bật SSL không (Prod = true, Dev = false)
        boolean isSsl = redisProperties.getSsl().isEnabled();
        
        // 2. Chọn giao thức: "rediss" (có s) cho Upstash, "redis" cho Local
        String protocol = isSsl ? "rediss" : "redis";

        String host = redisProperties.getHost();
        int port = redisProperties.getPort();
        String password = redisProperties.getPassword();

        // 3. Tạo chuỗi kết nối chuẩn (URI)
        // Format chuẩn: protocol://:password@host:port
        String redisUri;
        if (password != null && !password.isEmpty()) {
            // Trường hợp có mật khẩu (Production / Upstash)
            redisUri = String.format("%s://:%s@%s:%d", protocol, password, host, port);
        } else {
            // Trường hợp không mật khẩu (Localhost mặc định)
            redisUri = String.format("%s://%s:%d", protocol, host, port);
        }

        return RedisClient.create(redisUri);
    }

    @Bean
    public ProxyManager<String> lettuceProxyManager(RedisClient redisClient) {
        // Tạo kết nối Stateful
        StatefulRedisConnection<String, byte[]> connection = redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );

        return LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(60))
                )
                .build();
    }
}