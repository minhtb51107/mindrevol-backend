package com.mindrevol.backend.modules.journey.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "journey_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JourneyTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id", nullable = false)
    @ToString.Exclude
    private Journey journey;

    @Column(name = "day_no", nullable = false)
    private Integer dayNo; // Ngày thứ mấy (1, 2, 3...)

    @Column(nullable = false)
    private String title; // Tên nhiệm vụ

    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả chi tiết
}