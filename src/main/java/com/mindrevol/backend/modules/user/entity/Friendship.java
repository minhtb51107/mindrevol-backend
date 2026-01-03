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

    // [UUID] String ID từ BaseEntity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendshipStatus status;

    public boolean involvesUser(String userId) { // [UUID] String
        return this.requester.getId().equals(userId) || this.addressee.getId().equals(userId);
    }
    
    public User getFriend(String authorId) { // [UUID] String
        if (requester.getId().equals(authorId)) {
            return addressee;
        }
        return requester;
    }
}