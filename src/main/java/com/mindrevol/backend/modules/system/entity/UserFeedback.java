package com.mindrevol.backend.modules.system.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFeedback extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackType type; // Bây giờ nó đã hiểu FeedbackType là public enum

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String appVersion;
    
    private String deviceName;

    @Column(columnDefinition = "TEXT")
    private String screenshotUrl;
    
    @Builder.Default
    private boolean isResolved = false;
}