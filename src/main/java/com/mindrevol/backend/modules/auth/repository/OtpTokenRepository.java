package com.mindrevol.backend.modules.auth.repository;

import com.mindrevol.backend.modules.auth.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    // Tìm mã OTP mới nhất của user (để verify)
    Optional<OtpToken> findByUserId(Long userId);
    
    // Xóa mã cũ của user trước khi tạo mã mới
    void deleteByUserId(Long userId);
    
 // Thêm dòng này vào trong interface
    void deleteByExpiresAtBefore(OffsetDateTime now);
}