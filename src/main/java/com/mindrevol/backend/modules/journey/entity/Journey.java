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
    private boolean hasStreak = true; 

    @Column(name = "setting_req_freeze_ticket", nullable = false)
    @Builder.Default
    private boolean requiresFreezeTicket = true; 

    @Column(name = "setting_is_hardcore", nullable = false)
    @Builder.Default
    private boolean isHardcore = true; 
    
    @Column(name = "require_approval", nullable = false)
    @Builder.Default
    private boolean requireApproval = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false)
    @Builder.Default
    private InteractionType interactionType = InteractionType.GROUP_DISCUSS; 

    // --- MỚI: Template Fields ---
    @Column(name = "is_template", nullable = false)
    @Builder.Default
    private boolean isTemplate = false;

    @Column(name = "cloned_from_id")
    private UUID clonedFromId;
    // ---------------------------
    
 // --- MỚI: Cài đặt yêu cầu xác thực ảnh ---
    @Column(name = "setting_req_verification", nullable = false)
    @Builder.Default
    private boolean requiresVerification = false; 
    // ----------------------------------------

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

    // Helper method để tạo bản sao (Fork)
    public Journey copyForUser(User newCreator) {
        return Journey.builder()
                .name(this.name) 
                .description(this.description)
                .type(this.type)
                .status(JourneyStatus.ACTIVE)
                .theme(this.theme)
                .hasStreak(this.hasStreak)
                .requiresFreezeTicket(this.requiresFreezeTicket)
                .isHardcore(this.isHardcore)
                .interactionType(this.interactionType)
                .requireApproval(false) // Clone về thì mặc định public, không cần duyệt
                .creator(newCreator)
                .clonedFromId(this.id) // Link tới bản gốc
                .isTemplate(false) // Bản sao thì không phải template nữa
                .build();
    }
}