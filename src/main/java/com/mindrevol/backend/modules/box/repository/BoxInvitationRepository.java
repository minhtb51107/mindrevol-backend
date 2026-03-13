package com.mindrevol.backend.modules.box.repository;

import com.mindrevol.backend.modules.box.entity.BoxInvitation;
import com.mindrevol.backend.modules.journey.entity.JourneyInvitationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoxInvitationRepository extends JpaRepository<BoxInvitation, Long> {
    
    // Kiểm tra xem có lời mời nào đang chờ xử lý giữa Box và User này không
    boolean existsByBoxIdAndInviteeIdAndStatus(String boxId, String inviteeId, JourneyInvitationStatus status);

    // Lấy một lời mời đang chờ xử lý
    Optional<BoxInvitation> findByBoxIdAndInviteeIdAndStatus(String boxId, String inviteeId, JourneyInvitationStatus status);

    // [SỬA LỖI Ở ĐÂY] Thêm EntityGraph để nạp sẵn dữ liệu Box và Inviter, tránh lỗi LazyInitializationException
    @EntityGraph(attributePaths = {"box", "inviter"})
    List<BoxInvitation> findAllByInviteeIdAndStatusOrderByCreatedAtDesc(String inviteeId, JourneyInvitationStatus status);
}