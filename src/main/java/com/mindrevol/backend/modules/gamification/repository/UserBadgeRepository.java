package com.mindrevol.backend.modules.gamification.repository;

import com.mindrevol.backend.modules.gamification.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

// [FIX] Extends Long
@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, String> {
    
    boolean existsByUserIdAndBadgeId(String string, String string2);

    // createdAt là trường của BaseEntity
    List<UserBadge> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT ub.badge.id FROM UserBadge ub WHERE ub.user.id = :userId")
    Set<Long> findBadgeIdsByUserId(@Param("userId") String string);
    
    List<UserBadge> findByUserId(String userId);
}