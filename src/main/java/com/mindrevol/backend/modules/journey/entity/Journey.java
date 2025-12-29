package com.mindrevol.backend.modules.journey.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@Entity
@Table(name = "journeys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE journeys SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Journey extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JourneyVisibility visibility = JourneyVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JourneyStatus status = JourneyStatus.ONGOING; // [FIXED] Dùng ONGOING

    @Column(name = "invite_code", unique = true)
    private String inviteCode;

    // Giữ lại setting này để quản lý duyệt thành viên
    @Column(name = "require_approval")
    @Builder.Default
    private boolean requireApproval = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;
}