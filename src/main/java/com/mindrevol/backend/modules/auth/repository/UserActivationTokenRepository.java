package com.mindrevol.backend.modules.auth.repository;

import com.mindrevol.backend.modules.auth.entity.UserActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

// [UUID] JpaRepository<UserActivationToken, String>
@Repository
public interface UserActivationTokenRepository extends JpaRepository<UserActivationToken, String> {
    Optional<UserActivationToken> findByToken(String token);
}