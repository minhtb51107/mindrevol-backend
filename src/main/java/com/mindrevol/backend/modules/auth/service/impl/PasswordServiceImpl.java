package com.mindrevol.backend.modules.auth.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.common.service.AsyncTaskProducer;
import com.mindrevol.backend.modules.auth.dto.request.ChangePasswordRequest;
import com.mindrevol.backend.modules.auth.dto.request.CreatePasswordRequest;
import com.mindrevol.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.mindrevol.backend.modules.auth.dto.request.ResetPasswordRequest;
import com.mindrevol.backend.modules.auth.entity.PasswordResetToken;
import com.mindrevol.backend.modules.auth.repository.PasswordResetTokenRepository;
import com.mindrevol.backend.modules.auth.service.PasswordService;
import com.mindrevol.backend.modules.notification.dto.EmailTask;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordServiceImpl implements PasswordService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    // ĐÃ XÓA OtpTokenRepository
    
    private final PasswordEncoder passwordEncoder;
    private final AsyncTaskProducer asyncTaskProducer;
    private final RedisTemplate<String, Object> redisTemplate; // Thêm RedisTemplate

    // Constants Key Redis (Phải khớp với bên AuthServiceImpl để dùng chung OTP)
    private static final String OTP_PREFIX = "auth:otp:code:";       
    private static final String OTP_RETRY_PREFIX = "auth:otp:retry:";
    private static final String OTP_LIMIT_PREFIX = "auth:otp:limit:";

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            PasswordResetToken token = new PasswordResetToken(user);
            passwordResetTokenRepository.save(token);
            String link = "http://localhost:5173/reset-password?token=" + token.getToken();
            EmailTask task = EmailTask.builder().toEmail(user.getEmail()).subject("Reset Password").content("Link: " + link).retryCount(0).build();
            asyncTaskProducer.submitEmailTask(task);
        });
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken()).orElseThrow(() -> new BadRequestException("Invalid Token"));
        if (token.isExpired()) throw new BadRequestException("Expired Token");
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.delete(token);
    }

    @Override
    public void changePassword(ChangePasswordRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        if (user.getPassword() != null && !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu cũ không đúng.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public boolean hasPassword(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return "LOCAL".equals(user.getAuthProvider());
    }

    @Override
    public void createPassword(CreatePasswordRequest request, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setAuthProvider("LOCAL"); 
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updatePasswordWithOtp(String email, String otpCode, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        // --- BẮT ĐẦU LOGIC REDIS ---
        String otpKey = OTP_PREFIX + email;
        String retryKey = OTP_RETRY_PREFIX + email;

        // 1. Kiểm tra OTP có tồn tại trong Redis không
        String cachedOtp = (String) redisTemplate.opsForValue().get(otpKey);
        
        if (cachedOtp == null) {
            throw new BadRequestException("Mã OTP đã hết hạn hoặc chưa được gửi.");
        }

        // 2. Kiểm tra Retry Count (Số lần nhập sai)
        Integer retryCount = (Integer) redisTemplate.opsForValue().get(retryKey);
        if (retryCount == null) retryCount = 0;

        if (retryCount >= 5) {
            redisTemplate.delete(otpKey); // Xóa OTP
            redisTemplate.delete(retryKey);
            throw new BadRequestException("Bạn nhập sai quá 5 lần. Vui lòng yêu cầu mã mới.");
        }

        // 3. So sánh mã OTP
        if (!cachedOtp.equals(otpCode)) {
            // Nếu sai -> Tăng biến đếm
            redisTemplate.opsForValue().increment(retryKey);
            redisTemplate.expire(retryKey, 5, TimeUnit.MINUTES); // Gia hạn thời gian sống cho key retry
            throw new BadRequestException("Mã OTP không chính xác. (Sai " + (retryCount + 1) + "/5 lần)");
        }

        // 4. Nếu đúng -> Dọn dẹp Redis
        redisTemplate.delete(otpKey);
        redisTemplate.delete(retryKey);
        redisTemplate.delete(OTP_LIMIT_PREFIX + email); // Xóa luôn rate limit để user thoải mái thao tác tiếp

        // --- CẬP NHẬT MẬT KHẨU ---
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setAuthProvider("LOCAL"); 
        userRepository.save(user);
        
        log.info("User {} updated password via Redis OTP", email);
    }
}