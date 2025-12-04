package com.mindrevol.backend.modules.checkin.repository;

import com.mindrevol.backend.modules.checkin.entity.CheckinVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CheckinVerificationRepository extends JpaRepository<CheckinVerification, UUID> {
    
    // Đếm số phiếu CHẤP THUẬN cho 1 bài check-in
    @Query("SELECT COUNT(v) FROM CheckinVerification v WHERE v.checkin.id = :checkinId AND v.isApproved = true")
    long countApprovals(@Param("checkinId") UUID checkinId);

    // Đếm số phiếu TỪ CHỐI cho 1 bài check-in
    @Query("SELECT COUNT(v) FROM CheckinVerification v WHERE v.checkin.id = :checkinId AND v.isApproved = false")
    long countRejections(@Param("checkinId") UUID checkinId);

    // Kiểm tra xem user này đã vote cho bài này chưa
    boolean existsByCheckinIdAndVoterId(UUID checkinId, Long voterId);
}