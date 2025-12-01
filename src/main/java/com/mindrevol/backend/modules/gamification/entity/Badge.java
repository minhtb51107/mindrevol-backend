package com.mindrevol.backend.modules.gamification.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "badges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    private String name;
    private String description;
    private String iconUrl;

    @Enumerated(EnumType.STRING)
    private BadgeConditionType conditionType;

    private Integer conditionValue;
}