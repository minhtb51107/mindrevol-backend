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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Hàm quan trọng nhất: Tạo và Gửi thông báo
     * @param recipientId: ID người nhận
     * @param senderId: ID người gửi (có thể null nếu là System)
     * @param type: Loại thông báo
     * @param title: Tiêu đề
     * @param message: Nội dung
     * @param referenceId: ID liên kết (taskId, journeyId...)
     * @param imageUrl: Link ảnh thumbnail (nếu có)
     */
    @Async // Chạy bất đồng bộ để không làm chậm tác vụ chính
    @Transactional
    public void sendAndSaveNotification(Long recipientId, Long senderId, NotificationType type,
                                        String title, String message, String referenceId, String imageUrl) {
        
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

        // 2. Gửi FCM Push Notification (Mobile App)
        // TODO: Tích hợp FirebaseMessaging.getInstance().send(...) tại đây
        // Bạn có thể dùng recipient.getFcmToken() để lấy token thiết bị
        if (recipient.getFcmToken() != null) {
            log.info("Sending FCM to User {}: {} - {}", recipient.getId(), title, message);
            // firebaseService.send(recipient.getFcmToken(), title, message, dataPayload);
        } else {
            log.warn("User {} has no FCM Token, cannot push.", recipient.getId());
        }
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
            // Silent fail hoặc throw exception tùy ý
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
                .createdAt(n.getCreatedAt().toLocalDateTime())
                .senderId(n.getSender() != null ? n.getSender().getId() : null)
                .senderName(n.getSender() != null ? n.getSender().getFullname() : "System")
                .build();
    }
}