package com.mindrevol.backend.modules.journey.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "journey_invitations",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"journey_id", "invitee_id", "status"}) 
        // Đảm bảo không spam mời cùng 1 người vào 1 journey nhiều lần khi status còn PENDING
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JourneyInvitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id", nullable = false)
    private Journey journey;

    // Người gửi lời mời
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    // Người được mời
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JourneyInvitationStatus status;
}