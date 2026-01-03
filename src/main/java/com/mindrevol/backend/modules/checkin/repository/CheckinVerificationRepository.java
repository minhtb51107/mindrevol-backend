package com.mindrevol.backend.modules.checkin.repository;

import com.mindrevol.backend.modules.checkin.entity.CheckinVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// [FIX] Extends Long
@Repository
public interface CheckinVerificationRepository extends JpaRepository<CheckinVerification, String> {
    
    // [FIX] checkinId UUID -> Long
    @Query("SELECT COUNT(v) FROM CheckinVerification v WHERE v.checkin.id = :checkinId AND v.isApproved = true")
    long countApprovals(@Param("checkinId") Long checkinId);

    // [FIX] checkinId UUID -> Long
    @Query("SELECT COUNT(v) FROM CheckinVerification v WHERE v.checkin.id = :checkinId AND v.isApproved = false")
    long countRejections(@Param("checkinId") String string);

    // [FIX] checkinId UUID -> Long
    boolean existsByCheckinIdAndVoterId(String checkinId, String string);
}