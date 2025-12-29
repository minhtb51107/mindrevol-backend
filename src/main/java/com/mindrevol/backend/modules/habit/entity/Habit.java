package com.mindrevol.backend.modules.habit.entity;

import jakarta.persistence.*;
import lombok.*;
import com.mindrevol.backend.common.entity.BaseEntity; // Kiểm tra lại package BaseEntity của bạn
import com.mindrevol.backend.modules.user.entity.User;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "habits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Không cần @EntityListeners(AuditingEntityListener.class) nữa vì BaseEntity đã có
public class Habit extends BaseEntity {

    // Đã xóa id UUID
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(length = 20)
    private String frequency; // DAILY, WEEKLY

    private LocalTime reminderTime;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(name = "journey_id")
    private Long journeyId; // [FIX] Đổi từ UUID sang Long để khớp với bảng Journeys mới

    @Column(name = "is_archived")
    @Builder.Default
    private boolean archived = false;

    // Đã xóa createdAt, updatedAt vì BaseEntity tự lo
}