package com.mindrevol.backend.modules.auth.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "social_accounts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider", "provider_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String provider; // GOOGLE, FACEBOOK, APPLE

    @Column(name = "provider_id", nullable = false)
    private String providerId; // ID định danh từ phía MXH (VD: sub của Google)

    private String email; // Email của tài khoản MXH đó (có thể khác email chính)
    
    @Column(name = "avatar_url")
    private String avatarUrl;
}