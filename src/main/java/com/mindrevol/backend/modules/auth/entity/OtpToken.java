package com.mindrevol.backend.modules.auth.entity;

import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

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
    @UuidGenerator
    @Column(length = 36)
    private String id; // [UUID] Long -> String

    @Column(nullable = false)
    private String otpCode; 

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0; 

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(this.expiresAt);
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public static OtpToken create(User user) {
        String code = String.format("%06d", new Random().nextInt(999999));
        
        return OtpToken.builder()
                .user(user)
                .otpCode(code)
                .retryCount(0)
                .expiresAt(OffsetDateTime.now().plusMinutes(5)) 
                .build();
    }
}