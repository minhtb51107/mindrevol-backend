package com.mindrevol.backend.modules.notification.listener;

import com.mindrevol.backend.common.event.CheckinSuccessEvent;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.event.CommentPostedEvent;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.FirebaseService;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import com.mindrevol.backend.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final FirebaseService firebaseService; // [QUAN TRỌNG] Service bắn tin
    private final CheckinRepository checkinRepository;
    private final JourneyParticipantRepository participantRepository;

    // --- 1. XỬ LÝ KHI CÓ BÀI ĐĂNG MỚI (CHECK-IN) ---
    // Đây là tính năng quan trọng nhất để kéo user quay lại app
    @Async
    @EventListener
    @Transactional(readOnly = true)
    public void handleCheckinSuccess(CheckinSuccessEvent event) {
        log.info("🔔 Processing Notification for Checkin: {}", event.getCheckinId());

        Checkin checkin = checkinRepository.findById(event.getCheckinId()).orElse(null);
        if (checkin == null) return;

        User author = checkin.getUser();
        String journeyName = checkin.getJourney().getName();
        String journeyId = checkin.getJourney().getId();

        // Lấy tất cả thành viên trong hành trình (để thông báo cho họ)
        List<JourneyParticipant> participants = participantRepository.findAllByJourneyId(journeyId);

        for (JourneyParticipant p : participants) {
            User recipient = p.getUser();

            // Không gửi cho chính tác giả
            if (recipient.getId().equals(author.getId())) continue;

            String title = "Khoảnh khắc mới! 📸";
            String body = author.getFullname() + " vừa check-in trong " + journeyName;

            // 1. Lưu vào Database (Tab thông báo)
            notificationService.sendAndSaveNotification(
                    recipient.getId(),      // Người nhận
                    author.getId(),         // Người gây ra (Actor) (Lưu ý: API cũ của bạn nhận String ID)
                    NotificationType.CHECKIN,
                    title,
                    body,
                    checkin.getId(),        // Target ID (để click vào xem chi tiết)
                    checkin.getImageUrl()   // Thumbnail
            );

            // 2. Bắn Push Notification (Ting ting trên điện thoại)
            if (recipient.getFcmToken() != null) {
                Map<String, String> data = new HashMap<>();
                data.put("type", "CHECKIN");
                data.put("targetId", checkin.getId());
                data.put("journeyId", journeyId);
                
                firebaseService.sendNotification(recipient.getFcmToken(), title, body, data);
            }
        }
    }

    // --- 2. XỬ LÝ KHI CÓ BÌNH LUẬN MỚI ---
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentPosted(CommentPostedEvent event) {
        Checkin checkin = event.getCheckin();
        User commenter = event.getCommenter();
        User postOwner = checkin.getUser();

        // Chỉ gửi thông báo nếu người comment khác người đăng bài
        if (!postOwner.getId().equals(commenter.getId())) {
            
            String title = "Bình luận mới 💬";
            String body = commenter.getFullname() + ": " + event.getContent();

            // 1. Lưu DB
            notificationService.sendAndSaveNotification(
                    postOwner.getId(),
                    commenter.getId(),
                    NotificationType.COMMENT,
                    title,
                    body,
                    checkin.getId(),
                    commenter.getAvatarUrl()
            );
            
            // 2. Bắn Push Notification
            if (postOwner.getFcmToken() != null) {
                Map<String, String> data = new HashMap<>();
                data.put("type", "COMMENT");
                data.put("targetId", checkin.getId());
                
                firebaseService.sendNotification(postOwner.getFcmToken(), title, body, data);
            }

            log.info("Sent notification for comment on checkin {}", checkin.getId());
        }
    }
}