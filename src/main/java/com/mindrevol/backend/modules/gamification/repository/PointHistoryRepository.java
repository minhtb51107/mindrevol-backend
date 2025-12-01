package com.mindrevol.backend.modules.gamification.repository;

import com.mindrevol.backend.modules.gamification.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}