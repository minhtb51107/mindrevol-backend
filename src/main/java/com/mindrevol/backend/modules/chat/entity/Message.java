package com.mindrevol.backend.modules.chat.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_conversation_created", columnList = "conversation_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // [FIX] Dùng SuperBuilder để kế thừa BaseEntity
public class Message extends BaseEntity {

    // [FIX] Đã xóa @Id Long id (BaseEntity đã có)

    // ID tạm do Client sinh ra (UUID string) để Optimistic UI
    @Column(name = "client_side_id") 
    private String clientSideId; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    // [QUAN TRỌNG] Phải map quan hệ User để Mapper lấy được avatar/name
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType type; // TEXT, IMAGE, VIDEO, SYSTEM

    // Metadata (JSON) - Hibernate 6+
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    private Map<String, Object> metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status")
    @Builder.Default
    private MessageDeliveryStatus deliveryStatus = MessageDeliveryStatus.SENT;

    @Column(name = "is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    // Reply trực tiếp tin nhắn khác (Quote)
    // Lưu ý: Nếu muốn lấy nội dung tin được reply, nên map @ManyToOne Message replyToMsg
    // Nhưng để đơn giản thì lưu ID cũng được, tùy logic frontend.
    @Column(name = "reply_to_msg_id")
    private Long replyToMsgId;
}