package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.JourneyInvitation;
import com.mindrevol.backend.modules.journey.entity.JourneyInvitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JourneyInvitationRepository extends JpaRepository<JourneyInvitation, String> {

    boolean existsByJourneyIdAndInviteeIdAndStatus(String journeyId, String inviteeId, JourneyInvitationStatus status);

    // 1. Query lấy danh sách (Đã sửa ở bước trước)
    @Query("SELECT ji FROM JourneyInvitation ji " +
           "WHERE ji.invitee.id = :userId " +
           "AND ji.status = 'PENDING' " +
           "AND ji.journey.status IN ('ACTIVE', 'ONGOING', 'UPCOMING') " +
           "ORDER BY ji.createdAt DESC")
    @EntityGraph(attributePaths = {"journey", "inviter"})
    Page<JourneyInvitation> findPendingInvitationsForUser(@Param("userId") String userId, Pageable pageable);
    
    Optional<JourneyInvitation> findByIdAndInviteeId(String id, String inviteeId);

    // [FIX QUAN TRỌNG] Sửa hàm đếm này thành @Query tùy chỉnh
    // Thay vì để JPA tự gen câu SQL đơn giản, ta ép nó phải check trạng thái hành trình
    @Query("SELECT COUNT(ji) FROM JourneyInvitation ji " +
           "WHERE ji.invitee.id = :userId " +
           "AND ji.status = :status " +
           "AND ji.journey.status IN ('ACTIVE', 'ONGOING', 'UPCOMING')")
    long countByInviteeIdAndStatus(@Param("userId") String userId, @Param("status") JourneyInvitationStatus status);
}