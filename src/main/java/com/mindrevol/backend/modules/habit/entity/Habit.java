package com.mindrevol.backend.modules.habit.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "habits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Habit extends BaseEntity {

    // [UUID] ID String từ BaseEntity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(length = 20)
    private String frequency; 

    private LocalTime reminderTime;

    private LocalDate startDate;
    private LocalDate endDate;

    // [UUID] String (ID của Journey)
    @Column(name = "journey_id")
    private String journeyId; 

    @Column(name = "is_archived")
    @Builder.Default
    private boolean archived = false;
}