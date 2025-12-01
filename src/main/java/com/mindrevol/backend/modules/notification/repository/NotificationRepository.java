package com.mindrevol.backend.modules.notification.repository;

import com.mindrevol.backend.modules.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Lấy danh sách thông báo của user
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    // Đếm số thông báo chưa đọc (để hiện badge đỏ trên icon chuông)
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    // Đánh dấu tất cả là đã đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :userId")
    void markAllAsRead(Long userId);
}