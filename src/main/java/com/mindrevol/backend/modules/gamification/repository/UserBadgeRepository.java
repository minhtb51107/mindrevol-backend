package com.mindrevol.backend.modules.gamification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mindrevol.backend.modules.gamification.entity.UserBadge;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {
    // Kiểm tra xem user đã có badge này chưa (tránh trao trùng)
    boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);

    // Lấy danh sách huy hiệu của user để hiển thị Profile
    List<UserBadge> findByUserIdOrderByEarnedAtDesc(Long userId);
}