package com.mindrevol.backend.modules.checkin.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "checkin_verifications", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"checkin_id", "voter_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // [FIX] Dùng SuperBuilder để kế thừa BaseEntity
public class CheckinVerification extends BaseEntity {

    // [FIX] Đã xóa @Id UUID id (BaseEntity lo ID Long)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_id", nullable = false)
    private Checkin checkin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;

    // true = Chấp nhận (Hợp lệ), false = Từ chối (Fake/Spam)
    @Column(name = "is_approved", nullable = false)
    private boolean isApproved;

    // Optional: Lý do vote (nếu sau này cần mở rộng)
    @Column(columnDefinition = "TEXT")
    private String feedback;
    
    // [FIX] Đã xóa createdAt, reviewedAt (Dùng createdAt của BaseEntity)
}