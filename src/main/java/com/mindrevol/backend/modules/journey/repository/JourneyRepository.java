package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.Journey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JourneyRepository extends JpaRepository<Journey, UUID> {
    
    // Tìm journey bằng invite code (Dùng khi user nhập mã để join)
    Optional<Journey> findByInviteCode(String inviteCode);
    
    // Kiểm tra invite code đã tồn tại chưa (Dùng khi tạo mới để tránh trùng)
    boolean existsByInviteCode(String inviteCode);
}