package com.mindrevol.backend.modules.mood.repository;

import com.mindrevol.backend.modules.mood.entity.MoodReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MoodReactionRepository extends JpaRepository<MoodReaction, String> {
    Optional<MoodReaction> findByMoodIdAndUserId(String moodId, String userId);
    
    // Thêm hàm xóa tất cả reaction của 1 mood
    void deleteAllByMoodId(String moodId);
}