package com.mindrevol.backend.modules.checkin.repository;

import com.mindrevol.backend.modules.checkin.entity.CheckinReaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckinReactionRepository extends JpaRepository<CheckinReaction, Long> { // [FIX] Long

    // [FIX] UUID -> Long
    Optional<CheckinReaction> findByCheckinIdAndUserId(Long checkinId, Long userId);

    @Query("SELECT r FROM CheckinReaction r JOIN FETCH r.user WHERE r.checkin.id = :checkinId ORDER BY r.createdAt DESC")
    List<CheckinReaction> findLatestByCheckinId(@Param("checkinId") Long checkinId, Pageable pageable);

    long countByCheckinId(Long checkinId);
}