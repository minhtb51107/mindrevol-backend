package com.mindrevol.backend.modules.gamification.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "point_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PointHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int amount; // Có thể âm hoặc dương

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointSource source; // CHECKIN, JOIN_JOURNEY...

    @Column(columnDefinition = "TEXT")
    private String description;
    
    // ID của đối tượng nguồn (ví dụ ID bài checkin), lưu String UUID
    @Column(name = "reference_id") 
    private String referenceId; 
}