package com.mindrevol.backend.modules.notification.listener;

import com.mindrevol.backend.modules.checkin.event.CommentPostedEvent;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    // Lắng nghe sự kiện Comment
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // Chỉ gửi noti khi transaction lưu comment thành công
    public void handleCommentPosted(CommentPostedEvent event) {
        // Logic cũ từ Service chuyển sang đây
        if (!event.getCheckin().getUser().getId().equals(event.getCommenter().getId())) {
            notificationService.sendAndSaveNotification(
                    event.getCheckin().getUser().getId(),
                    event.getCommenter().getId(),
                    NotificationType.COMMENT,
                    "Bình luận mới 💬",
                    event.getCommenter().getFullname() + " đã bình luận: " + event.getContent(),
                    event.getCheckin().getId().toString(),
                    event.getCommenter().getAvatarUrl()
            );
            log.info("Sent notification for comment on checkin {}", event.getCheckin().getId());
        }
    }
}