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
public interface JourneyInvitationRepository extends JpaRepository<JourneyInvitation, String> { // [UUID]

    // [UUID] String journeyId, String inviteeId
    boolean existsByJourneyIdAndInviteeIdAndStatus(String journeyId, String inviteeId, JourneyInvitationStatus status);

    // [UUID] userId là String
    @Query("SELECT ji FROM JourneyInvitation ji WHERE ji.invitee.id = :userId AND ji.status = 'PENDING' ORDER BY ji.createdAt DESC")
    @EntityGraph(attributePaths = {"journey", "inviter"})
    Page<JourneyInvitation> findPendingInvitationsForUser(@Param("userId") String userId, Pageable pageable);
    
    Optional<JourneyInvitation> findByIdAndInviteeId(String id, String inviteeId);

	long countByInviteeIdAndStatus(String userId, JourneyInvitationStatus pending);
}