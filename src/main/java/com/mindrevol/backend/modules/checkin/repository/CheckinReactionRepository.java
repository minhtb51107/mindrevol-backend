package com.mindrevol.backend.modules.checkin.repository;

import com.mindrevol.backend.modules.checkin.entity.CheckinReaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CheckinReactionRepository extends JpaRepository<CheckinReaction, UUID> { // Sửa ID thành UUID

    // Tìm reaction cũ của user
    Optional<CheckinReaction> findByCheckinIdAndUserId(UUID checkinId, Long userId);

    // Lấy danh sách reaction (JOIN FETCH user để tối ưu hiệu năng cho FacePile/Modal)
    @Query("SELECT r FROM CheckinReaction r JOIN FETCH r.user WHERE r.checkin.id = :checkinId ORDER BY r.createdAt DESC")
    List<CheckinReaction> findLatestByCheckinId(@Param("checkinId") UUID checkinId, Pageable pageable);

    // Đếm tổng số reaction
    long countByCheckinId(UUID checkinId);
}