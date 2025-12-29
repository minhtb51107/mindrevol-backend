package com.mindrevol.backend.modules.habit.entity;

import jakarta.persistence.*;
import lombok.*;
import com.mindrevol.backend.common.entity.BaseEntity; // Kiểm tra lại package BaseEntity

import java.time.LocalDate;

@Entity
@Table(name = "habit_logs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"habit_id", "log_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Không cần @EntityListeners nữa
public class HabitLog extends BaseEntity {

    // Đã xóa id UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(name = "checkin_id")
    private Long checkinId; // [FIX] Đổi từ UUID sang Long (Link tới bảng Checkin dùng ID số)

    @Column(nullable = false)
    private LocalDate logDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HabitLogStatus status; // COMPLETED, FAILED, SKIPPED

    // Đã xóa createdAt
}