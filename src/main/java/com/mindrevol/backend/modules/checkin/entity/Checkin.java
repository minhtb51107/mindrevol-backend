package com.mindrevol.backend.modules.checkin.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
@SuperBuilder // [QUAN TRỌNG] Dùng SuperBuilder để kế thừa ID Long từ BaseEntity
public class Checkin extends BaseEntity {

    // [ĐÃ XÓA] @Id UUID id (BaseEntity đã lo ID Long)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id", nullable = false)
    private Journey journey;

    // [ĐÃ XÓA] private JourneyTask task; (Vì đã bỏ tính năng Task)

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(length = 50) 
    private String emotion; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CheckinStatus status = CheckinStatus.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CheckinVisibility visibility = CheckinVisibility.PUBLIC;

    // BaseEntity đã có createdAt, nhưng ta cần checkinDate riêng cho logic business
    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    @OneToMany(mappedBy = "checkin", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CheckinComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "checkin", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CheckinReaction> reactions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        // BaseEntity sẽ tự set createdAt
        if (this.checkinDate == null) this.checkinDate = LocalDate.now();
    }
}