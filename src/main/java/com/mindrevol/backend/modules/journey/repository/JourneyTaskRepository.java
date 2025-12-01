package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.JourneyTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JourneyTaskRepository extends JpaRepository<JourneyTask, UUID> {
    // Hiện tại chưa cần query gì phức tạp, JpaRepository đã đủ dùng
}