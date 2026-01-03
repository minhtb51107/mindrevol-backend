package com.mindrevol.backend.modules.system.repository;
import com.mindrevol.backend.modules.system.entity.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, String> {
}