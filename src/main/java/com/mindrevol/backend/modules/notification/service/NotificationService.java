package com.mindrevol.backend.modules.notification.service;

import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.notification.dto.response.NotificationResponse;
import com.mindrevol.backend.modules.notification.entity.Notification;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.repository.NotificationRepository;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate; 
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.concurrent.TimeUnit; 
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate; 
    private final FirebaseService firebaseService;
    private final SimpMessagingTemplate messagingTemplate;

    @Async 
    @Transactional
    public void sendAndSaveNotification(Long recipientId, Long senderId, NotificationType type,
                                        String title, String message, String referenceId, String imageUrl) {
        
        // --- LOGIC MỚI: CHỐNG SPAM (THROTTLING) ---
        if (type == NotificationType.REACTION || type == NotificationType.COMMENT) {
            String throttleKey = "noti_throttle:" + recipientId + ":" + type + ":" + referenceId;
            
            if (Boolean.TRUE.equals(redisTemplate.hasKey(throttleKey))) {
                log.info("Spam protection: Skipped notification for user {} type {} ref {}", recipientId, type, referenceId);
                return; 
            }
            
            redisTemplate.opsForValue().set(throttleKey, "1", 30, TimeUnit.MINUTES);
        }
        // ------------------------------------------

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));

        User sender = null;
        if (senderId != null) {
            sender = userRepository.findById(senderId).orElse(null);
        }

        // 1. Lưu vào Database
        Notification notification = Notification.builder()
                .recipient(recipient)
                .sender(sender)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .imageUrl(imageUrl)
                .isRead(false)
                .build();
        
        notificationRepository.save(notification);
        
        if (recipient.getFcmToken() != null) {
            Map<String, String> dataPayload = new HashMap<>();
            dataPayload.put("type", type.name());
            if (referenceId != null) dataPayload.put("referenceId", referenceId);
            if (sender != null) dataPayload.put("senderId", sender.getId().toString());
            if (imageUrl != null) dataPayload.put("imageUrl", imageUrl);

            firebaseService.sendNotification(
                recipient.getFcmToken(), 
                title, 
                message, 
                dataPayload
            );
        }

        // --- 2. Gửi WebSocket ---
        NotificationResponse response = toResponse(notification);
        
        messagingTemplate.convertAndSendToUser(
                recipient.getEmail(), 
                "/queue/notifications", 
                response
        );
    }

    public Page<NotificationResponse> getMyNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
        if (!notification.getRecipient().getId().equals(userId)) {
            return;
        }
        
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }
    
    public long countUnread(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .referenceId(n.getReferenceId())
                .imageUrl(n.getImageUrl())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt()) // [FIX] Bỏ .toLocalDateTime() đi
                .senderId(n.getSender() != null ? n.getSender().getId() : null)
                .senderName(n.getSender() != null ? n.getSender().getFullname() : "System")
                .build();
    }
}