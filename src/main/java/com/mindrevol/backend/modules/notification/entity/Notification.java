package com.mindrevol.backend.modules.notification.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    // Người nhận thông báo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    // Người tạo ra thông báo (có thể null nếu là System)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    // ID tham chiếu đến đối tượng liên quan (VD: ID của Journey, ID của Checkin)
    // Giúp App khi bấm vào noti thì navigate đến đúng màn hình
    private String referenceId;

    // Đường dẫn ảnh (avatar người gửi hoặc thumbnail checkin)
    private String imageUrl;

    @Column(nullable = false)
    private boolean isRead; // Đã xem chưa
}