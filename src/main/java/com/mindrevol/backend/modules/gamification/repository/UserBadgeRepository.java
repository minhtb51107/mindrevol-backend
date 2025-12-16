package com.mindrevol.backend.modules.gamification.repository;

import com.mindrevol.backend.modules.gamification.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {
    
    boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);

    List<UserBadge> findByUserIdOrderByEarnedAtDesc(Long userId);

    // --- QUERY MỚI: Lấy tất cả Badge ID user đã sở hữu (để check in-memory) ---
    @Query("SELECT ub.badge.id FROM UserBadge ub WHERE ub.user.id = :userId")
    Set<Long> findBadgeIdsByUserId(Long userId);
    
    List<UserBadge> findByUserId(Long userId);
}