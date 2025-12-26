package com.mindrevol.backend.modules.checkin.entity;

import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.entity.JourneyTask;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "checkins", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_checkin_user_journey_date", columnNames = {"user_id", "journey_id", "checkin_date"})
    },
    indexes = {
        @Index(name = "idx_checkin_journey", columnList = "journey_id"),
        @Index(name = "idx_checkin_user", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private JourneyTask task;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    // --- [SỬA ĐỔI] Dùng String để thoải mái lưu mọi loại emotion/emoji ---
    @Column(length = 50) 
    private String emotion; 
    // --------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckinStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CheckinVisibility visibility = CheckinVisibility.PUBLIC;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    // --- [GIỮ NGUYÊN] Để Mapper hoạt động ---
    @OneToMany(mappedBy = "checkin", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CheckinComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "checkin", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CheckinReaction> reactions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.checkinDate == null) this.checkinDate = this.createdAt.toLocalDate();
    }
}