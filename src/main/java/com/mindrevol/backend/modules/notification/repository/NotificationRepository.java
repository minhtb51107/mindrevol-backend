package com.mindrevol.backend.modules.notification.repository;

import com.mindrevol.backend.modules.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// [UUID] JpaRepository<Notification, String>
@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    // [UUID] userId là String
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(String userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :userId")
    void markAllAsRead(@Param("userId") String userId);
    
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipient.id = :userId")
    void deleteAllByRecipientId(@Param("userId") String userId);

    // Dành cho Job tự động dọn rác
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoffDate")
    void deleteOldReadNotifications(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}