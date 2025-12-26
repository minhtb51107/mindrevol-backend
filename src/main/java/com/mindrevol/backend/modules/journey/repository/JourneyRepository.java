package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.entity.JourneyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JourneyRepository extends JpaRepository<Journey, UUID> {
	
	// 1. Tìm các hành trình đang ACTIVE mà đã quá ngày kết thúc
    @Query("SELECT j FROM Journey j WHERE j.status = 'ACTIVE' AND j.endDate < :today")
    List<Journey> findExpiredActiveJourneys(@Param("today") LocalDate today);

    // 2. Lấy danh sách hành trình ĐÃ HOÀN THÀNH của User (Để hiện ở Tab Recap trên Profile)
    @Query("SELECT j FROM Journey j " +
           "JOIN JourneyParticipant jp ON j.id = jp.journey.id " +
           "WHERE jp.user.id = :userId AND j.status = 'COMPLETED' " +
           "ORDER BY j.endDate DESC")
    List<Journey> findCompletedJourneysByUserId(@Param("userId") Long userId);
    
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
    
 // --- [SỬA ĐỔI] Query update trạng thái hành trình hết hạn ---
    @Modifying
    @Query("UPDATE Journey j SET j.status = 'COMPLETED' WHERE j.status = 'ACTIVE' AND j.endDate < :today")
    int updateExpiredJourneysStatus(@Param("today") LocalDate today);
}