package com.mindrevol.backend.modules.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mindrevol.backend.modules.auth.entity.PasswordResetToken;

import java.util.Date;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Tìm token bằng chuỗi token.
     * @param token Chuỗi token duy nhất.
     * @return Optional chứa PasswordResetToken nếu tìm thấy.
     */
    Optional<PasswordResetToken> findByToken(String token);
    
    void deleteByExpiryDateBefore(Date now);
}