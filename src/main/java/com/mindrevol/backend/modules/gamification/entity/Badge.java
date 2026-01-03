package com.mindrevol.backend.modules.gamification.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "badges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Badge extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false)
    private BadgeConditionType conditionType;

    @Column(name = "condition_value", nullable = false)
    private int conditionValue; 
    
    // Ví dụ: conditionType = STREAK, conditionValue = 7 (Huy hiệu 7 ngày liên tiếp)
}