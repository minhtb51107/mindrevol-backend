package com.mindrevol.backend.modules.journey.entity;

import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "journeys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Journey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "invite_code", nullable = false, unique = true, length = 10)
    private String inviteCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JourneyType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JourneyStatus status = JourneyStatus.ACTIVE;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 50)
    @Builder.Default
    private String theme = "DEFAULT";
    
    @Column(name = "setting_has_streak", nullable = false)
    @Builder.Default
    private boolean hasStreak = true; // Có tính chuỗi không?

    @Column(name = "setting_req_freeze_ticket", nullable = false)
    @Builder.Default
    private boolean requiresFreezeTicket = true; // Nghỉ có mất vé không?

    @Column(name = "setting_is_hardcore", nullable = false)
    @Builder.Default
    private boolean isHardcore = true; // Nhắc nhở gắt hay nhẹ nhàng?
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User creator;

    @OneToMany(mappedBy = "journey", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<JourneyParticipant> participants = new HashSet<>();

    @OneToMany(mappedBy = "journey", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNo ASC") 
    @Builder.Default
    private List<JourneyTask> roadmap = new ArrayList<>();
}