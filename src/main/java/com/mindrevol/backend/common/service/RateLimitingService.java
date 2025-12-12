package com.mindrevol.backend.common.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final ProxyManager<String> proxyManager;

    @Value("${app.ratelimit.login.limit}") private long loginLimit;
    @Value("${app.ratelimit.login.duration-min}") private long loginDurationMin;

    @Value("${app.ratelimit.general.limit}") private long generalLimit;
    @Value("${app.ratelimit.general.duration-min}") private long generalDurationMin;

    @Value("${app.ratelimit.strict.limit}") private long strictLimit;
    @Value("${app.ratelimit.strict.duration-hour}") private long strictDurationHour;

    // --- MỚI: OTP LIMIT ---
    @Value("${app.ratelimit.otp.limit:5}") private long otpLimit;
    @Value("${app.ratelimit.otp.duration-min:1}") private long otpDurationMin;

    public Bucket resolveLoginBucket(String ip) {
        String key = "rate_limit:login:" + ip;
        return proxyManager.builder().build(key, loginConfigSupplier());
    }

    public Bucket resolveGeneralBucket(String ip) {
        String key = "rate_limit:general:" + ip;
        return proxyManager.builder().build(key, generalConfigSupplier());
    }

    public Bucket resolveStrictBucket(String ip) {
        String key = "rate_limit:strict:" + ip;
        return proxyManager.builder().build(key, strictConfigSupplier());
    }

    // --- MỚI ---
    public Bucket resolveOtpBucket(String ip) {
        String key = "rate_limit:otp:" + ip;
        return proxyManager.builder().build(key, () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(otpLimit, Refill.greedy(otpLimit, Duration.ofMinutes(otpDurationMin))))
                .build());
    }

    private Supplier<BucketConfiguration> loginConfigSupplier() {
        return () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(loginLimit, Refill.greedy(loginLimit, Duration.ofMinutes(loginDurationMin))))
                .build();
    }

    private Supplier<BucketConfiguration> generalConfigSupplier() {
        return () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(generalLimit, Refill.greedy(generalLimit, Duration.ofMinutes(generalDurationMin))))
                .build();
    }

    private Supplier<BucketConfiguration> strictConfigSupplier() {
        return () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(strictLimit, Refill.greedy(strictLimit, Duration.ofHours(strictDurationHour))))
                .build();
    }
}