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
    List<Mood> findByBoxIdAndExpiresAtAfterOrderByUpdatedAtDesc(String boxId, LocalDateTime now);
    Optional<Mood> findByBoxIdAndUserId(String boxId, String userId);

    // [VÁ LỖ HỔNG 3] Dùng Query Native/JPQL để XÓA VĨNH VIỄN (Hard delete)
    @Modifying
    @Query(value = "DELETE FROM mood_reactions WHERE mood_id IN (SELECT id FROM moods WHERE expires_at < :time)", nativeQuery = true)
    void hardDeleteExpiredReactions(@Param("time") LocalDateTime time);

    @Modifying
    @Query(value = "DELETE FROM moods WHERE expires_at < :time", nativeQuery = true)
    void hardDeleteExpiredMoods(@Param("time") LocalDateTime time);
}