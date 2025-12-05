package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.entity.JourneyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JourneyRepository extends JpaRepository<Journey, UUID> {
    
    Optional<Journey> findByInviteCode(String inviteCode);
    
    boolean existsByInviteCode(String inviteCode);

    // Tìm tất cả hành trình do User này làm chủ (Creator)
    List<Journey> findByCreatorId(Long creatorId);

    // --- MỚI: Đếm số hành trình đang hoạt động của 1 người ---
    // Dùng để kiểm tra giới hạn (Limiters)
    long countByCreatorIdAndStatus(Long creatorId, JourneyStatus status);

    // --- MỚI: Tìm các hành trình mẫu (Template) ---
    // Chỉ lấy những cái được đánh dấu là template và đang active
    @Query("SELECT j FROM Journey j WHERE j.isTemplate = true AND j.status = 'ACTIVE'")
    List<Journey> findAllTemplates();
}