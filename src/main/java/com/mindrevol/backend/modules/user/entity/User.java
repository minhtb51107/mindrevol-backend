package com.mindrevol.backend.modules.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.auth.entity.SocialAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
public class User extends BaseEntity implements UserDetails {

    // [UUID] ID kế thừa từ BaseEntity (String)

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
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

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
    private String timezone = "UTC"; 
    
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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SocialAccount> socialAccounts = new HashSet<>();

    // --- [NEW] CÁC TRƯỜNG CHO SUBSCRIPTION ---

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    @Builder.Default
    private AccountType accountType = AccountType.FREE;

    // Ngày hết hạn gói VIP (Null nếu là FREE hoặc vĩnh viễn)
    @Column(name = "subscription_expiry_date")
    private LocalDateTime subscriptionExpiryDate;

    /**
     * Logic kiểm tra xem User có phải là VIP (GOLD) và còn hạn sử dụng hay không.
     */
    public boolean isPremium() {
        // 1. Phải là loại tài khoản GOLD (hoặc cao hơn)
        if (this.accountType != AccountType.GOLD && this.accountType != AccountType.PLATINUM) {
            return false;
        }
        // 2. Nếu ngày hết hạn là null -> Coi như vĩnh viễn (hoặc lỗi, tùy logic, ở đây mình coi là chưa kích hoạt)
        // Nhưng logic an toàn: Gold mà null date thì check logic nạp tiền.
        // Ở đây ta quy định: Đã là GOLD thì phải có ExpiryDate hợp lệ.
        return this.subscriptionExpiryDate != null && this.subscriptionExpiryDate.isAfter(LocalDateTime.now());
    }

    // -----------------------------------------

    @Version
    private Long version;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getUsername() { return this.email; }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return this.status == UserStatus.ACTIVE; }
}