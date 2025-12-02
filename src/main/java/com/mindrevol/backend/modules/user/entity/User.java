package com.mindrevol.backend.modules.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mindrevol.backend.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_handle", columnList = "handle")
})
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL") 
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_ACTIVATION;

    @Column(nullable = false, length = 50, unique = true)
    private String handle;

    @Column(nullable = false, length = 100)
    private String fullname;

    @Column(length = 500)
    private String avatarUrl;

    @Column(length = 255)
    private String bio;

    @Column(length = 255)
    private String website;

    @Column(length = 20)
    @Builder.Default
    private String authProvider = "LOCAL";
    
    @Column(name = "fcm_token")
    private String fcmToken;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    
    @Column(columnDefinition = "bigint default 0")
    @Builder.Default
    private Long points = 0L;

    @Column(name = "freeze_streak_count", columnDefinition = "int default 0")
    @Builder.Default
    private Integer freezeStreakCount = 0;

    // --- THÊM MỚI: OPTIMISTIC LOCKING ---
    @Version
    private Long version;
    // ------------------------------------
}