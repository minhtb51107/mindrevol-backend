package com.mindrevol.backend.modules.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    @Column(name = "media_url")
    private String mediaUrl;

    // --- LOCKET STYLE CORE ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_checkin_id")
    private Checkin replyToCheckin; // Tin nhắn này là reply cho ảnh nào?

    @Column(name = "is_read")
    @Builder.Default
    private boolean isRead = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}