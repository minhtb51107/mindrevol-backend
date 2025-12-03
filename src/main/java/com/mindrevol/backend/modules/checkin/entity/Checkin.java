package com.mindrevol.backend.modules.checkin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.entity.JourneyTask;
import com.mindrevol.backend.modules.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "checkins", indexes = {
        @Index(name = "idx_checkin_journey", columnList = "journey_id"),
        @Index(name = "idx_checkin_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id", nullable = false)
    private Journey journey;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Emotion emotion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckinStatus status;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CheckinVisibility visibility = CheckinVisibility.PUBLIC;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_task_id") // Có thể null nếu là Habit
    private JourneyTask task;
}