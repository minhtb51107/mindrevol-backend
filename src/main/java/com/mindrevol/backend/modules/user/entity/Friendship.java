package com.mindrevol.backend.modules.user.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "friendships",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"requester_id", "addressee_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friendship extends BaseEntity {

    // Người gửi lời mời kết bạn
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    // Người nhận lời mời
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendshipStatus status;

    /**
     * Helper method để kiểm tra user có tham gia vào friendship này không
     */
    public boolean involvesUser(Long userId) {
        return this.requester.getId().equals(userId) || this.addressee.getId().equals(userId);
    }
    
    /**
     * Helper method để lấy ra User đối phương (người kia)
     */
    public User getFriend(Long myUserId) {
        if (requester.getId().equals(myUserId)) {
            return addressee;
        }
        return requester;
    }
}