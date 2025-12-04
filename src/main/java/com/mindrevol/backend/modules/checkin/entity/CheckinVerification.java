package com.mindrevol.backend.modules.checkin.entity;

import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "checkin_verifications", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"checkin_id", "voter_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CheckinVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_id", nullable = false)
    private Checkin checkin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;

    // true = Chấp nhận (Hợp lệ), false = Từ chối (Fake/Spam)
    @Column(name = "is_approved", nullable = false)
    private boolean isApproved;

    @CreatedDate
    private LocalDateTime createdAt;
}