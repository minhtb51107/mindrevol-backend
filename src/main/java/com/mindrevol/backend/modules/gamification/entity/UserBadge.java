package com.mindrevol.backend.modules.gamification.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

// [FIX] Đã xóa import java.util.UUID;

@Entity
@Table(name = "user_badges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserBadge extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "journey_id")
    private Long journeyId; 
    
    // createdAt có sẵn trong BaseEntity
}