package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.JourneyInvitation;
import com.mindrevol.backend.modules.journey.entity.JourneyInvitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JourneyInvitationRepository extends JpaRepository<JourneyInvitation, Long> {

    // Kiểm tra xem đã có lời mời nào đang PENDING cho người này vào journey này chưa
    boolean existsByJourneyIdAndInviteeIdAndStatus(UUID journeyId, Long inviteeId, JourneyInvitationStatus status);

    // Lấy danh sách lời mời ĐẾN tôi (Invitee = Me, Status = PENDING)
    @Query("SELECT ji FROM JourneyInvitation ji WHERE ji.invitee.id = :userId AND ji.status = 'PENDING' ORDER BY ji.createdAt DESC")
    @EntityGraph(attributePaths = {"journey", "inviter"}) // [FIX] Thêm dòng này
    Page<JourneyInvitation> findPendingInvitationsForUser(@Param("userId") Long userId, Pageable pageable);
    
    // Tìm lời mời cụ thể để xử lý (Accept/Reject)
    Optional<JourneyInvitation> findByIdAndInviteeId(Long id, Long inviteeId);
}