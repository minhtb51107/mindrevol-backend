package com.mindrevol.backend.modules.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mindrevol.backend.modules.auth.entity.MagicLinkToken;

import java.util.Optional;

@Repository
public interface MagicLinkTokenRepository extends JpaRepository<MagicLinkToken, Long> {
    Optional<MagicLinkToken> findByToken(String token);
}