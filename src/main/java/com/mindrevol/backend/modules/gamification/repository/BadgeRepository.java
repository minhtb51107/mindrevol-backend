package com.mindrevol.backend.modules.gamification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mindrevol.backend.modules.gamification.entity.Badge;
import com.mindrevol.backend.modules.gamification.entity.BadgeConditionType;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {
    Optional<Badge> findByCode(String code);
    
    // Lấy danh sách badge theo điều kiện (VD: Lấy hết badge loại STREAK để check)
    List<Badge> findByConditionType(BadgeConditionType conditionType);
}