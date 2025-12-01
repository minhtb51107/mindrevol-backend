package com.mindrevol.backend.modules.checkin.repository;

import com.mindrevol.backend.modules.checkin.entity.Checkin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CheckinRepository extends JpaRepository<Checkin, UUID> {

    // TỐI ƯU: Sử dụng JOIN FETCH để lấy luôn User và Task trong 1 lần gọi (Khắc phục lỗi N+1)
    // Query này áp dụng cho cả chế độ Nhẹ và Nặng đều tốt.
    @Query("SELECT c FROM Checkin c " +
           "JOIN FETCH c.user " +          // Lấy luôn User
           "LEFT JOIN FETCH c.task " +     // Lấy luôn Task (nếu có)
           "WHERE c.journey.id = :journeyId " +
           "ORDER BY c.createdAt DESC")
    Page<Checkin> findByJourneyIdOrderByCreatedAtDesc(@Param("journeyId") UUID journeyId, Pageable pageable);

    Optional<Checkin> findTopByJourneyIdAndUserIdOrderByCreatedAtDesc(UUID journeyId, Long userId);

    boolean existsByUserIdAndTaskId(Long userId, UUID taskId);

    @Query("SELECT c.task.id FROM Checkin c WHERE c.user.id = :userId AND c.journey.id = :journeyId AND c.task IS NOT NULL")
    Set<UUID> findCompletedTaskIdsByUserAndJourney(@Param("userId") Long userId, @Param("journeyId") UUID journeyId);

	List<Checkin> findByJourneyIdAndUserId(UUID journeyId, Long id);
}