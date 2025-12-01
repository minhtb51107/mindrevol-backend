package com.mindrevol.backend.modules.journey.entity;

import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "journey_participants", 
uniqueConstraints = {@UniqueConstraint(columnNames = {"journey_id", "user_id"})},
indexes = {
    @Index(name = "idx_participant_user", columnList = "user_id"),
    @Index(name = "idx_participant_journey_user", columnList = "journey_id, user_id") 
}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JourneyParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id", nullable = false)
    @ToString.Exclude 
    private Journey journey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude 
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JourneyRole role = JourneyRole.MEMBER; 

    @Column(name = "joined_at", nullable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "current_streak")
    @Builder.Default
    private Integer currentStreak = 0;
    
    @Column(name = "last_checkin_at")
    private LocalDate lastCheckinAt;
}