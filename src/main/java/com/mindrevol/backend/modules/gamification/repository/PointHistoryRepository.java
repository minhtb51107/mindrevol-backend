package com.mindrevol.backend.modules.gamification.repository;

import com.mindrevol.backend.modules.gamification.entity.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, String> { // [UUID]
    // [UUID] userId là String
    Page<PointHistory> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}