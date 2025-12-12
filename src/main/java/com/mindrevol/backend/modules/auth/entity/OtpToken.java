package com.mindrevol.backend.modules.auth.entity;

import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.Random;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "otp_tokens")
public class OtpToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String otpCode; // Mã 6 số: "123456"

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0; // Đếm số lần nhập sai

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(this.expiresAt);
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    // Hàm tiện ích để tạo mã mới
    public static OtpToken create(User user) {
        // Sinh ngẫu nhiên 6 số
        String code = String.format("%06d", new Random().nextInt(999999));
        
        return OtpToken.builder()
                .user(user)
                .otpCode(code)
                .retryCount(0)
                // Mã chỉ sống trong 5 phút
                .expiresAt(OffsetDateTime.now().plusMinutes(5)) 
                .build();
    }
}