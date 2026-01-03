package com.mindrevol.backend.modules.auth.repository;

import com.mindrevol.backend.modules.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.Optional;

// [UUID] JpaRepository<PasswordResetToken, String>
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByExpiresAtBefore(OffsetDateTime now);
}