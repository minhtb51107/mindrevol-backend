package com.mindrevol.backend.modules.auth.repository;

import com.mindrevol.backend.modules.auth.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// [UUID] JpaRepository<SocialAccount, String>
@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, String> {
    Optional<SocialAccount> findByProviderAndProviderId(String provider, String providerId);
    
    // [UUID] userId là String
    List<SocialAccount> findAllByUserId(String userId);
    
    // [UUID] userId là String
    Optional<SocialAccount> findByUserIdAndProvider(String userId, String provider);
    
    // [UUID] userId là String
    long countByUserId(String userId);
}