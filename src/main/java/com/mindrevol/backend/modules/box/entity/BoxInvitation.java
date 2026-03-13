package com.mindrevol.backend.modules.box.entity;

import com.mindrevol.backend.modules.journey.entity.JourneyInvitationStatus; // Dùng chung Enum trạng thái với Journey cho đồng bộ
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "box_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoxInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_id", nullable = false)
    private Box box;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JourneyInvitationStatus status; // PENDING, ACCEPTED, REJECTED, EXPIRED

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}