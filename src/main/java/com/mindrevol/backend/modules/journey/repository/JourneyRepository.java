package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.Journey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JourneyRepository extends JpaRepository<Journey, UUID> {
    
    Optional<Journey> findByInviteCode(String inviteCode);
    
    boolean existsByInviteCode(String inviteCode);

    // --- MỚI: Tìm tất cả hành trình do User này làm chủ (Creator) ---
    List<Journey> findByCreatorId(Long creatorId);
}