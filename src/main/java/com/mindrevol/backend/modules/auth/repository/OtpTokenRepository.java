package com.mindrevol.backend.modules.auth.repository;

import com.mindrevol.backend.modules.auth.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.Optional;

// [UUID] JpaRepository<OtpToken, String>
@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, String> {
    // [UUID] userId là String
    Optional<OtpToken> findByUserId(String userId);
    
    // [UUID] userId là String
    void deleteByUserId(String userId);
    
    void deleteByExpiresAtBefore(OffsetDateTime now);
}