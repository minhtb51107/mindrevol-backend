package com.mindrevol.backend.modules.notification.service;

import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.notification.dto.response.NotificationResponse;
import com.mindrevol.backend.modules.notification.entity.Notification;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.repository.NotificationRepository;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.entity.UserSettings;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository; // [THÊM] Import Cài đặt người dùng
    private final RedisTemplate<String, Object> redisTemplate;
    private final FirebaseService firebaseService;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @Transactional
    public void sendAndSaveNotification(String recipientId, String senderId, NotificationType type,
                                        String title, String message, String referenceId, String imageUrl) {
        
        // 1. Chống Spam
        if (type == NotificationType.REACTION || type == NotificationType.COMMENT) {
            String throttleKey = "noti_throttle:" + recipientId + ":" + type + ":" + referenceId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(throttleKey))) {
                log.info("Spam protection: Skipped notification for user {} type {} ref {}", recipientId, type, referenceId);
                return;
            }
            redisTemplate.opsForValue().set(throttleKey, "1", 30, TimeUnit.MINUTES);
        }

        // 2. Lấy thông tin người nhận & cài đặt của họ
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));
                
        UserSettings settings = userSettingsRepository.findByUserId(recipientId)
                .orElseGet(() -> UserSettings.builder().user(recipient).build());

        // 3. Kiểm tra xem có được phép Push Notification theo cài đặt không
        boolean isPushAllowed = checkPushSettings(type, settings);

        User sender = null;
        if (senderId != null) {
            sender = userRepository.findById(senderId).orElse(null);
        }

        // 4. Luôn lưu vào Database (để hiển thị trong chuông thông báo trong app)
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
        
        NotificationResponse response = toResponse(notification);

        // 5. Chỉ gửi Push (FCM & WebSocket Popup) nếu người dùng bật cài đặt
        if (isPushAllowed) {
            // Gửi FCM Push tới điện thoại
            if (recipient.getFcmToken() != null) {
                Map<String, String> dataPayload = new HashMap<>();
                dataPayload.put("type", type.name());
                if (referenceId != null) dataPayload.put("referenceId", referenceId);
                if (sender != null) dataPayload.put("senderId", sender.getId());
                if (imageUrl != null) dataPayload.put("imageUrl", imageUrl);

                firebaseService.sendNotification(
                    recipient.getFcmToken(), 
                    title, 
                    message, 
                    dataPayload
                );
            }
            // Gửi qua WebSocket
            messagingTemplate.convertAndSendToUser(
                    recipient.getId(), 
                    "/queue/notifications", 
                    response
            );
        }
    }

    // Hàm phụ trợ map Loại thông báo (NotificationType) với Cài đặt (UserSettings)
    private boolean checkPushSettings(NotificationType type, UserSettings settings) {
        if (type == null) return true;
        return switch (type) {
            case FRIEND_REQUEST -> settings.isPushFriendRequest();
            case COMMENT -> settings.isPushNewComment();
            case REACTION -> settings.isPushReaction();
            case JOURNEY_INVITE -> settings.isPushJourneyInvite();
            default -> true; // Các loại thông báo quan trọng mặc định bật
        };
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getRecipient().getId().equals(userId)) return;
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsRead(userId);
    }
    
    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }
    
    @Transactional
    public void deleteNotification(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo"));
        if (notification.getRecipient().getId().equals(userId)) {
            notificationRepository.delete(notification);
        }
    }

    @Transactional
    public void deleteAllMyNotifications(String userId) {
        notificationRepository.deleteAllByRecipientId(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType() != null ? n.getType().name() : null) 
                .referenceId(n.getReferenceId())
                .imageUrl(n.getImageUrl())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt()) 
                .senderId(n.getSender() != null ? n.getSender().getId() : null)
                .senderName(n.getSender() != null ? n.getSender().getFullname() : "Hệ thống")
                .build();
    }
}