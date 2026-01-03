package com.mindrevol.backend.modules.auth.repository;

import com.mindrevol.backend.modules.auth.entity.MagicLinkToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

// [UUID] JpaRepository<MagicLinkToken, String>
@Repository
public interface MagicLinkTokenRepository extends JpaRepository<MagicLinkToken, String> {
    Optional<MagicLinkToken> findByToken(String token);
}