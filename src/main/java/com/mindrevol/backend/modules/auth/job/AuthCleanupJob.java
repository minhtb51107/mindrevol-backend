package com.mindrevol.backend.modules.auth.job;

import com.mindrevol.backend.modules.auth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthCleanupJob {

    private final OtpTokenRepository otpTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    // private final MagicLinkTokenRepository magicLinkTokenRepository; // Nếu có dùng

    // Chạy mỗi ngày lúc 3:00 sáng
    // cron = "Giây Phút Giờ Ngày Tháng Thứ"
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional // Để cho phép xóa dữ liệu
    public void cleanupExpiredTokens() {
        log.info("🧹 Bắt đầu dọn dẹp Token rác...");

        // 1. Xóa OTP hết hạn
        // OffsetDateTime.now() lấy giờ hiện tại. Xóa tất cả cái nào hạn < giờ hiện tại.
        otpTokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());

        // 2. Xóa Reset Password Token hết hạn
        passwordResetTokenRepository.deleteByExpiryDateBefore(new Date());

        log.info("✅ Đã dọn dẹp xong Token rác.");
    }
}