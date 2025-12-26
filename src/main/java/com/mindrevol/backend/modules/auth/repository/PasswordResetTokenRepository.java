package com.mindrevol.backend.modules.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mindrevol.backend.modules.auth.entity.PasswordResetToken;
import java.time.OffsetDateTime; // Import đúng kiểu thời gian
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);
    
    void deleteByExpiresAtBefore(OffsetDateTime now);
}