package com.mindrevol.backend.modules.mood.repository;

import com.mindrevol.backend.modules.mood.entity.Mood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoodRepository extends JpaRepository<Mood, String> {
    
    // Lấy các status Mood (tâm trạng) trong một Box mà VẪN CÒN HIỆU LỰC (chưa hết hạn/expiresAt)
    List<Mood> findByBoxIdAndExpiresAtAfterOrderByUpdatedAtDesc(String boxId, LocalDateTime now);
    
    // Lấy Mood hiện tại của một user trong Box
    Optional<Mood> findByBoxIdAndUserId(String boxId, String userId);

    // [VÁ LỖ HỔNG] Job dọn dẹp hệ thống:
    // Dùng Native SQL để XÓA VĨNH VIỄN (Hard Delete) các lượt Reaction (Thả emoji) của những Mood đã hết hạn (tiết kiệm dung lượng DB)
    @Modifying
    @Query(value = "DELETE FROM mood_reactions WHERE mood_id IN (SELECT id FROM moods WHERE expires_at < :time)", nativeQuery = true)
    void hardDeleteExpiredReactions(@Param("time") LocalDateTime time);

    // XÓA VĨNH VIỄN (Hard Delete) các Mood đã quá hạn (ví dụ: Mood dạng story tự xóa sau 24h)
    @Modifying
    @Query(value = "DELETE FROM moods WHERE expires_at < :time", nativeQuery = true)
    void hardDeleteExpiredMoods(@Param("time") LocalDateTime time);
}