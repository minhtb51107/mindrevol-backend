package com.mindrevol.backend.common.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

    // Inject ProxyManager đã cấu hình ở bước 2
    private final ProxyManager<String> proxyManager;

    /**
     * Bucket cho Login/Register: Khắt khe (5 req / 1 phút)
     */
    public Bucket resolveLoginBucket(String ip) {
        String key = "rate_limit:login:" + ip;
        return proxyManager.builder().build(key, loginConfigSupplier());
    }

    /**
     * Bucket cho API chung: Thoáng (100 req / 1 phút)
     */
    public Bucket resolveGeneralBucket(String ip) {
        String key = "rate_limit:general:" + ip;
        return proxyManager.builder().build(key, generalConfigSupplier());
    }

    // --- CẤU HÌNH BUCKET (Dạng Supplier) ---

    private Supplier<BucketConfiguration> loginConfigSupplier() {
        return () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
                .build();
    }

    private Supplier<BucketConfiguration> generalConfigSupplier() {
        return () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
                .build();
    }
}