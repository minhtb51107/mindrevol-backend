package com.mindrevol.backend.modules.gamification.repository;

import com.mindrevol.backend.modules.gamification.entity.Badge;
import com.mindrevol.backend.modules.gamification.entity.BadgeConditionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, String> { // [UUID]
    Optional<Badge> findByName(String name);
    List<Badge> findByConditionType(BadgeConditionType conditionType);
}