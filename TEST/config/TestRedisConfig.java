package com.mindrevol.backend.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.lettuce.core.RedisClient;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.cache.CacheManager;

import static org.mockito.Mockito.mock;

/**
 * Test configuration để Mock tất cả Redis beans
 * Tránh lỗi kết nối thực đến Redis trong test
 */
@TestConfiguration
public class TestRedisConfig {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        return mock(RedisTemplate.class);
    }

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return mock(CacheManager.class);
    }

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        return mock(RedissonClient.class);
    }

    @Bean
    @Primary
    public RedisClient redisClient() {
        return mock(RedisClient.class);
    }

    @Bean
    @Primary
    public ProxyManager<String> lettuceProxyManager(RedisClient redisClient) {
        return mock(ProxyManager.class);
    }
}



