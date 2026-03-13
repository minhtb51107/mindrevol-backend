package com.mindrevol.backend.modules.chat.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_user1_user2", columnList = "user1_id, user2_id"),
    @Index(name = "idx_last_message_at", columnList = "last_message_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Conversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    // --- Caching for Inbox ---
    @Column(name = "last_message_content")
    private String lastMessageContent; 

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "last_sender_id")
    private String lastSenderId; 

    // --- Read Receipts ---
    @Column(name = "user1_last_read_msg_id")
    private String user1LastReadMessageId; 

    @Column(name = "user2_last_read_msg_id")
    private String user2LastReadMessageId; 

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;

    // [THÊM MỚI] Gắn liên kết với Box. Nếu là Chat 1-1 thì trường này null.
    @Column(name = "box_id")
    private String boxId;
}