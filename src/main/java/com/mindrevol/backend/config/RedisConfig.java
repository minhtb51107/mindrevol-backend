package com.mindrevol.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Tạo Serializer tùy chỉnh có hỗ trợ Java 8 Date/Time (LocalDate, LocalDateTime)
     */
    private GenericJackson2JsonRedisSerializer createSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        // 1. Đăng ký Module để xử lý ngày tháng (Fix lỗi SerializationException)
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 2. Lưu thông tin Class vào JSON để khi đọc ra biết là object gì (Quan trọng)
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Sử dụng Serializer đã cấu hình ở trên
        GenericJackson2JsonRedisSerializer jsonSerializer = createSerializer();
        
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // Cũng phải dùng Serializer xịn này cho CacheManager để tránh lỗi tương tự khi cache dữ liệu
        GenericJackson2JsonRedisSerializer jsonSerializer = createSerializer();

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)); // <--- Thay đổi ở đây

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Widget cần refresh nhanh (5 phút)
        cacheConfigurations.put("journey_widget", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Profile user ít thay đổi hơn (30 phút)
        cacheConfigurations.put("user_profile", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}