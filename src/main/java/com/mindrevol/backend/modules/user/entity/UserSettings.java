package com.mindrevol.backend.modules.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore; // <--- THÊM IMPORT
import com.mindrevol.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore // <--- THÊM DÒNG NÀY (Quan trọng: Ngắt vòng lặp JSON)
    private User user;

    // ... (Giữ nguyên các trường còn lại: emailDailyReminder, pushFriendRequest...)
    @Column(name = "email_daily_reminder")
    @Builder.Default
    private boolean emailDailyReminder = true;

    @Column(name = "email_updates")
    @Builder.Default
    private boolean emailUpdates = true;

    @Column(name = "push_friend_request")
    @Builder.Default
    private boolean pushFriendRequest = true;

    @Column(name = "push_new_comment")
    @Builder.Default
    private boolean pushNewComment = true;
    
    @Column(name = "push_journey_invite")
    @Builder.Default
    private boolean pushJourneyInvite = true;
    
    @Column(name = "push_reaction")
    @Builder.Default
    private boolean pushReaction = true;
}