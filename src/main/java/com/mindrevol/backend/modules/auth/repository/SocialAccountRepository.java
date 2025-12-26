package com.mindrevol.backend.modules.auth.repository;

import com.mindrevol.backend.modules.auth.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndProviderId(String provider, String providerId);
    List<SocialAccount> findAllByUserId(Long userId);
    Optional<SocialAccount> findByUserIdAndProvider(Long userId, String provider);
    long countByUserId(Long userId);
}