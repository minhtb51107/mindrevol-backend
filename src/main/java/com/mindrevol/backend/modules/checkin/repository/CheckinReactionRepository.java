package com.mindrevol.backend.modules.checkin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mindrevol.backend.modules.checkin.entity.CheckinReaction;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CheckinReactionRepository extends JpaRepository<CheckinReaction, UUID> {
    // Tìm xem User (Long ID) đã thả tim vào Checkin (UUID) chưa
    Optional<CheckinReaction> findByCheckinIdAndUserId(UUID checkinId, Long userId);
    
    // Đếm số lượng reaction của 1 bài checkin
    int countByCheckinId(UUID checkinId);
}