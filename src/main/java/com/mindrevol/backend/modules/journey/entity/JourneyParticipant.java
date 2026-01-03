package com.mindrevol.backend.modules.journey.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "journey_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JourneyParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id", nullable = false)
    private Journey journey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JourneyRole role = JourneyRole.MEMBER;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "current_streak")
    @Builder.Default
    private int currentStreak = 0;

    @Column(name = "total_checkins")
    @Builder.Default
    private int totalCheckins = 0;

    // Vẫn giữ lại cột này vì nó hữu ích để đếm số ngày
    @Column(name = "total_active_days")
    @Builder.Default
    private int totalActiveDays = 0;

    @Column(name = "last_checkin_at")
    private LocalDateTime lastCheckinAt;
}