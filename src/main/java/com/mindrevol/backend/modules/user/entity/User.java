package com.mindrevol.backend.modules.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.auth.entity.SocialAccount;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate; // <-- Nhớ import cái này
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
    
    // --- MỚI: Thêm ngày sinh ---
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    // --------------------------

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
    
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String timezone = "UTC"; // Mặc định là UTC
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

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
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SocialAccount> socialAccounts = new HashSet<>();

    @Version
    private Long version;
}