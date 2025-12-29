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
public interface JourneyInvitationRepository extends JpaRepository<JourneyInvitation, Long> {

    // [FIX] Đổi UUID -> Long cho journeyId
    boolean existsByJourneyIdAndInviteeIdAndStatus(Long journeyId, Long inviteeId, JourneyInvitationStatus status);

    // Lấy danh sách lời mời ĐẾN tôi
    @Query("SELECT ji FROM JourneyInvitation ji WHERE ji.invitee.id = :userId AND ji.status = 'PENDING' ORDER BY ji.createdAt DESC")
    @EntityGraph(attributePaths = {"journey", "inviter"})
    Page<JourneyInvitation> findPendingInvitationsForUser(@Param("userId") Long userId, Pageable pageable);
    
    // Tìm lời mời cụ thể
    Optional<JourneyInvitation> findByIdAndInviteeId(Long id, Long inviteeId);
}