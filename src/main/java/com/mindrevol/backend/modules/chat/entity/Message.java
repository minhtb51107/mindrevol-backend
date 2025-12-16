package com.mindrevol.backend.modules.chat.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_conversation_created", columnList = "conversation_id, created_at") // Index cho phân trang tin nhắn
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Support Optimistic UI ---
    // ID tạm do Client sinh ra (UUID) lúc bấm gửi để tracking trạng thái
    @Column(name = "client_side_id") 
    private String clientSideId; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType type; // TEXT, IMAGE, VIDEO, SYSTEM

    // --- Metadata (JSON) ---
    // Lưu thông tin ngữ cảnh: reply_to_post_id, image_thumbnail...
    // Postgres sẽ lưu dạng JSONB, MySQL lưu dạng JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    private Map<String, Object> metadata;

    // --- Vòng đời tin nhắn ---
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status")
    @Builder.Default
    private MessageDeliveryStatus deliveryStatus = MessageDeliveryStatus.SENT; // SENT, DELIVERED, SEEN

    @Column(name = "is_deleted")
    @Builder.Default // <--- THÊM DÒNG NÀY
    private boolean isDeleted = false;

    // Reply trực tiếp tin nhắn khác (Quote)
    @Column(name = "reply_to_msg_id")
    private Long replyToMsgId;
}