package com.mindrevol.backend.modules.gamification.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "point_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long amount; // Số điểm thay đổi (Ví dụ: +10 hoặc -50)

    @Column(nullable = false)
    private Long balanceAfter; // Số dư sau khi thay đổi (Snapshot)

    @Column(nullable = false)
    private String reason; // Lý do (Vd: "Check-in hàng ngày", "Mua vé đóng băng")
    
    @Enumerated(EnumType.STRING)
    private PointSource source;
}