package com.mindrevol.backend.modules.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.common.service.AsyncTaskProducer;
import com.mindrevol.backend.common.utils.JwtUtil;
import com.mindrevol.backend.modules.auth.dto.RedisUserSession;
import com.mindrevol.backend.modules.auth.dto.RegisterTempData; // Import Class mới
import com.mindrevol.backend.modules.auth.dto.request.*;
import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.auth.dto.response.UserSessionResponse;
import com.mindrevol.backend.modules.auth.entity.*;
import com.mindrevol.backend.modules.auth.repository.*;
import com.mindrevol.backend.modules.auth.service.AuthService;
import com.mindrevol.backend.modules.auth.service.strategy.SocialLoginFactory;
import com.mindrevol.backend.modules.auth.service.strategy.SocialLoginStrategy;
import com.mindrevol.backend.modules.auth.service.strategy.SocialProviderData;
import com.mindrevol.backend.modules.auth.util.AppleAuthUtil;
import com.mindrevol.backend.modules.notification.dto.EmailTask;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.AccountType;
import com.mindrevol.backend.modules.user.entity.Gender;
import com.mindrevol.backend.modules.user.entity.Role;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.entity.UserStatus;
import com.mindrevol.backend.modules.user.repository.RoleRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MagicLinkTokenRepository magicLinkTokenRepository;
    private final UserActivationTokenRepository activationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final SocialAccountRepository socialAccountRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AsyncTaskProducer asyncTaskProducer;
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // [REFACTOR] Inject Factory
    private final SocialLoginFactory socialLoginFactory;
    
    // Inject các bean cũ (nếu còn dùng)
    private final RestTemplate restTemplate;
    private final AppleAuthUtil appleAuthUtil;
    private final ObjectMapper objectMapper;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;
    
    @Value("${tiktok.client-key:}") private String tiktokClientKey;
    @Value("${tiktok.client-secret:}") private String tiktokClientSecret;

    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";
    
    // --- CONSTANT CHO LUỒNG ĐĂNG KÝ MỚI ---
    private static final String REG_TEMP_PREFIX = "reg_temp:";
    private static final long REG_TEMP_TTL_MINUTES = 10;

    // ==================================================================================
    // NEW: UNIFIED SOCIAL LOGIN LOGIC (STRATEGY PATTERN)
    // ==================================================================================

    private JwtResponse processUnifiedSocialLogin(String providerName, Object requestData, HttpServletRequest servletRequest) {
        SocialLoginStrategy strategy = socialLoginFactory.getStrategy(providerName);
        SocialProviderData data = strategy.verifyAndGetData(requestData);
        User user = findOrCreateUser(providerName, data);
        return createTokenAndSession(user, servletRequest);
    }

    private User findOrCreateUser(String provider, SocialProviderData data) {
        Optional<SocialAccount> socialAccountOpt = socialAccountRepository.findByProviderAndProviderId(provider, data.getProviderId());
        if (socialAccountOpt.isPresent()) {
            return socialAccountOpt.get().getUser();
        }

        User user;
        Optional<User> existingUser = Optional.empty();
        if (data.getEmail() != null) {
            existingUser = userRepository.findByEmail(data.getEmail());
        }
        
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = createNewSocialUser(data.getEmail(), data.getName(), data.getAvatarUrl());
        }

        SocialAccount newLink = SocialAccount.builder()
                .user(user)
                .provider(provider)
                .providerId(data.getProviderId())
                .email(data.getEmail())
                .avatarUrl(data.getAvatarUrl())
                .build();
        socialAccountRepository.save(newLink);
        
        return user;
    }

    private User createNewSocialUser(String email, String name, String avatarUrl) {
        String safeEmail = (email != null) ? email : "no-email-" + UUID.randomUUID() + "@mindrevol.local";
        String baseHandle = safeEmail.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        if (baseHandle.isEmpty()) baseHandle = "user";
        
        String handle = baseHandle;
        int suffix = 1;
        while (userRepository.existsByHandle(handle)) {
            handle = baseHandle + "." + (++suffix);
        }
        
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));
        
        User newUser = User.builder()
                .email(safeEmail)
                .fullname(name != null ? name : "New User")
                .avatarUrl(avatarUrl)
                .handle(handle)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .status(UserStatus.ACTIVE)
                .gender(Gender.PREFER_NOT_TO_SAY)
                .accountType(AccountType.FREE)
                .authProvider("SOCIAL")
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();
        return userRepository.save(newUser);
    }

    // ==================================================================================
    // IMPLEMENTATION OF INTERFACE METHODS (DELEGATING TO STRATEGY)
    // ==================================================================================

    @Override
    public JwtResponse loginWithTikTok(TikTokLoginRequest request, HttpServletRequest servletRequest) {
        return processUnifiedSocialLogin("tiktok", request, servletRequest);
    }

    @Override
    public JwtResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest servletRequest) {
        return processUnifiedSocialLogin("google", request, servletRequest);
    }

    @Override
    public JwtResponse loginWithFacebook(FacebookLoginRequest request, HttpServletRequest servletRequest) {
        return processUnifiedSocialLogin("facebook", request, servletRequest);
    }

    @Override
    public JwtResponse loginWithApple(AppleLoginRequest request, HttpServletRequest servletRequest) {
        return processUnifiedSocialLogin("apple", request, servletRequest);
    }

    // ==================================================================================
    // NEW REGISTRATION FLOW (REDIS -> OTP -> DB)
    // ==================================================================================

    @Override
    public void registerUserStep1(RegisterRequest request) {
        // 1. Validate DB
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng bởi một tài khoản khác.");
        }
        if (userRepository.existsByHandle(request.getHandle())) {
            throw new BadRequestException("Handle @" + request.getHandle() + " đã tồn tại.");
        }
        if (request.getDateOfBirth() == null) throw new BadRequestException("Ngày sinh là bắt buộc.");

        // 2. Tạo OTP ngẫu nhiên (6 số)
        String otpCode = String.format("%06d", new Random().nextInt(999999));

        // 3. Chuẩn bị dữ liệu tạm (Hash password ngay tại đây để bảo mật)
        RegisterTempData tempData = RegisterTempData.builder()
                .fullname(request.getFullname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .handle(request.getHandle())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .otpCode(otpCode)
                .retryCount(0)
                .build();

        // 4. Lưu object vào Redis với Key là Email (Tự hủy sau 10 phút)
        String redisKey = REG_TEMP_PREFIX + request.getEmail();
        redisTemplate.opsForValue().set(redisKey, tempData, REG_TEMP_TTL_MINUTES, TimeUnit.MINUTES);

        // 5. Gửi Email OTP
        String subject = "Xác thực đăng ký tài khoản MindRevol";
        String content = "<div style='font-family: sans-serif; padding: 20px; color: #333;'>" +
                "<h2>Xin chào " + request.getFullname() + ",</h2>" +
                "<p>Cảm ơn bạn đã đăng ký tham gia MindRevol.</p>" +
                "<p>Mã xác thực (OTP) của bạn là:</p>" +
                "<h1 style='color: #4F46E5; letter-spacing: 5px; background: #f3f4f6; display: inline-block; padding: 10px 20px; border-radius: 8px;'>" + otpCode + "</h1>" +
                "<p>Mã này có hiệu lực trong vòng <b>10 phút</b>.</p>" +
                "<p style='font-size: 12px; color: #666;'>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.</p>" +
                "</div>";
        
        EmailTask task = EmailTask.builder()
                .toEmail(request.getEmail())
                .subject(subject)
                .content(content)
                .retryCount(0)
                .build();
        asyncTaskProducer.submitEmailTask(task);

        log.info("Step 1 Register: OTP sent to temp user {}", request.getEmail());
    }

    @Override
    public JwtResponse verifyRegisterOtp(VerifyRegisterOtpRequest request, HttpServletRequest servletRequest) {
        String redisKey = REG_TEMP_PREFIX + request.getEmail();
        
        // 1. Lấy dữ liệu tạm từ Redis
        RegisterTempData tempData = (RegisterTempData) redisTemplate.opsForValue().get(redisKey);

        if (tempData == null) {
            throw new BadRequestException("Mã xác thực đã hết hạn hoặc email không chính xác. Vui lòng đăng ký lại.");
        }

        // 2. Kiểm tra mã OTP
        if (!tempData.getOtpCode().equals(request.getOtpCode())) {
            int currentRetry = tempData.getRetryCount() + 1;
            tempData.setRetryCount(currentRetry);
            
            if (currentRetry > 5) {
                redisTemplate.delete(redisKey);
                throw new BadRequestException("Bạn đã nhập sai quá nhiều lần. Phiên đăng ký đã bị hủy.");
            }
            
            // Cập nhật lại Retry Count
            redisTemplate.opsForValue().set(redisKey, tempData, REG_TEMP_TTL_MINUTES, TimeUnit.MINUTES);
            throw new BadRequestException("Mã OTP không chính xác. Bạn còn " + (6 - currentRetry) + " lần thử.");
        }

        // 3. OTP Hợp lệ -> Tiến hành tạo User vào Database
        // Check race condition lần cuối
        if (userRepository.existsByHandle(tempData.getHandle())) {
            throw new BadRequestException("Rất tiếc, Handle @" + tempData.getHandle() + " vừa bị người khác đăng ký. Vui lòng chọn Handle khác.");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").description("Default user").build()));

        User newUser = User.builder()
                .email(tempData.getEmail())
                .password(tempData.getPassword()) // Password đã hash ở step 1
                .fullname(tempData.getFullname())
                .handle(tempData.getHandle())
                .dateOfBirth(tempData.getDateOfBirth())
                .gender(tempData.getGender())
                .status(UserStatus.ACTIVE) // ACTIVE LUÔN
                .accountType(AccountType.FREE)
                .authProvider("LOCAL")
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();

        userRepository.save(newUser);
        
        // Tạo Setting mặc định
        createDefaultSettings(newUser);

        // 4. Dọn dẹp Redis
        redisTemplate.delete(redisKey);

        // 5. Tự động đăng nhập
        return createTokenAndSession(newUser, servletRequest);
    }

    @Override
    public void resendRegisterOtp(ResendRegisterOtpRequest request) {
        String redisKey = REG_TEMP_PREFIX + request.getEmail();
        RegisterTempData tempData = (RegisterTempData) redisTemplate.opsForValue().get(redisKey);

        if (tempData == null) {
            throw new BadRequestException("Phiên đăng ký không tồn tại hoặc đã hết hạn. Vui lòng đăng ký lại.");
        }

        // Sinh OTP mới
        String newOtp = String.format("%06d", new Random().nextInt(999999));
        tempData.setOtpCode(newOtp);
        
        // Gia hạn TTL
        redisTemplate.opsForValue().set(redisKey, tempData, REG_TEMP_TTL_MINUTES, TimeUnit.MINUTES);

        // Gửi lại Email
        String subject = "Gửi lại mã xác thực - MindRevol";
        String content = "<p>Mã xác thực MỚI của bạn là: <b style='font-size: 20px; color: #4F46E5;'>" + newOtp + "</b></p>";
        
        EmailTask task = EmailTask.builder()
                .toEmail(tempData.getEmail())
                .subject(subject)
                .content(content)
                .retryCount(0)
                .build();
        asyncTaskProducer.submitEmailTask(task);
    }

    @Override
    public boolean isHandleExists(String handle) {
        return userRepository.existsByHandle(handle);
    }
    
    // Hàm này giữ lại để tham khảo hoặc legacy support (nhưng logic đã chuyển sang Step 1)
    // @Override 
    // public void registerUser(RegisterRequest request) { ... } 

    // ==================================================================================
    // OTHER AUTH METHODS
    // ==================================================================================

    @Override
    @Transactional
    public void sendOtpLogin(SendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email này chưa đăng ký tài khoản."));
        
        OtpToken otpToken = otpTokenRepository.findByUserId(user.getId())
                .orElseGet(() -> OtpToken.builder().user(user).build());

        // --- [NEW] LOGIC CHẶN SPAM (DÙNG TOÁN HỌC) ---
        // Logic: Thời điểm gửi = Thời gian hết hạn - 5 phút (Duration mặc định)
        // Nếu (Thời điểm gửi + 60s) vẫn còn ở tương lai -> Nghĩa là chưa qua 60s -> CHẶN
        
        if (otpToken.getExpiresAt() != null) {
            OffsetDateTime lastSentAt = otpToken.getExpiresAt().minusMinutes(5);
            
            if (lastSentAt.plusSeconds(60).isAfter(OffsetDateTime.now())) {
                 // Tính số giây còn lại để báo cho user biết
                 long secondsLeft = java.time.Duration.between(OffsetDateTime.now(), lastSentAt.plusSeconds(60)).getSeconds();
                 throw new BadRequestException("Vui lòng đợi " + secondsLeft + " giây trước khi gửi lại mã.");
            }
        }
        // ---------------------------------------------

        String newCode = String.format("%06d", new Random().nextInt(999999));
        otpToken.setOtpCode(newCode);
        otpToken.setRetryCount(0);
        
        // Cập nhật ExpiresAt (Lưu ý: Luôn phải khớp với số phút trừ ở trên)
        otpToken.setExpiresAt(OffsetDateTime.now().plusMinutes(5)); 
        
        otpTokenRepository.save(otpToken);

        // ... Đoạn gửi mail giữ nguyên ...
        String subject = "Mã xác thực đăng nhập MindRevol";
        String content = "<h1>Mã OTP của bạn: " + newCode + "</h1>";
        EmailTask task = EmailTask.builder().toEmail(user.getEmail()).subject(subject).content(content).retryCount(0).build();
        asyncTaskProducer.submitEmailTask(task);
        
        log.info("Sent OTP login to user: {}", user.getEmail());
    }

    @Override
    public JwtResponse verifyOtpLogin(VerifyOtpRequest request, HttpServletRequest servletRequest) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        OtpToken token = otpTokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Vui lòng yêu cầu gửi mã OTP trước."));
        if (token.isExpired()) {
            otpTokenRepository.delete(token);
            throw new BadRequestException("Mã OTP đã hết hạn.");
        }
        if (token.getRetryCount() >= 5) {
            otpTokenRepository.delete(token);
            throw new BadRequestException("Nhập sai quá nhiều lần.");
        }
        if (!token.getOtpCode().equals(request.getOtpCode())) {
            token.incrementRetry();
            otpTokenRepository.save(token);
            throw new BadRequestException("Mã OTP không chính xác.");
        }
        otpTokenRepository.delete(token);
        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }
        return createTokenAndSession(user, servletRequest);
    }

    @Override
    public UserSummaryResponse checkEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // [SỬA ĐỔI]: Chỉ coi là có mật khẩu nếu AuthProvider là LOCAL
            // Vì Social Account (Google/FB) vẫn có mật khẩu ngẫu nhiên trong DB
            boolean hasPass = "LOCAL".equalsIgnoreCase(user.getAuthProvider());
            
            // Nếu bạn muốn hỗ trợ trường hợp user Social ĐÃ thiết lập mật khẩu thêm sau này
            // thì cần logic phức tạp hơn, nhưng hiện tại hãy dùng cách này để fix lỗi trước mắt.
            
            return UserSummaryResponse.builder()
                    .id(user.getId())
                    .fullname(user.getFullname())
                    .handle(user.getHandle())
                    .avatarUrl(user.getAvatarUrl())
                    .isOnline(true)
                    .hasPassword(hasPass) // <--- Giá trị mới
                    .authProvider(user.getAuthProvider()) 
                    .build();
        }
        return null;
    }

    @Override
    public JwtResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) throw new DisabledException("Tài khoản bị khóa.");
        return createTokenAndSession(user, servletRequest);
    }

    @Override
    public void activateUserAccount(String token) {
        UserActivationToken activationToken = activationTokenRepository.findByToken(token).orElseThrow(() -> new BadRequestException("Token không hợp lệ."));
        if (activationToken.isExpired()) {
            activationTokenRepository.delete(activationToken);
            throw new BadRequestException("Token đã hết hạn.");
        }
        User user = activationToken.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        activationTokenRepository.delete(activationToken);
    }

    private JwtResponse createTokenAndSession(User user, HttpServletRequest request) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        RedisUserSession redisSession = RedisUserSession.builder().id(UUID.randomUUID().toString()).email(user.getEmail()).refreshToken(refreshToken).ipAddress(request.getRemoteAddr()).userAgent(request.getHeader("User-Agent")).expiredAt(System.currentTimeMillis() + refreshTokenExpirationMs).build();
        String redisKey = SESSION_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(redisKey, redisSession, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);
        String userSessionsKey = USER_SESSIONS_PREFIX + user.getEmail();
        redisTemplate.opsForSet().add(userSessionsKey, refreshToken);
        redisTemplate.expire(userSessionsKey, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);
        log.info("Saved session for user: {}", user.getEmail());
        return JwtResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build();
    }

    @Override
    public JwtResponse refreshToken(String refreshToken) {
        String redisKey = SESSION_PREFIX + refreshToken;
        RedisUserSession session = (RedisUserSession) redisTemplate.opsForValue().get(redisKey);
        if (session == null) throw new BadRequestException("Invalid Refresh Token");
        User user = userRepository.findByEmail(session.getEmail()).orElseThrow();
        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        String userSessionsKey = USER_SESSIONS_PREFIX + user.getEmail();
        redisTemplate.opsForSet().remove(userSessionsKey, refreshToken);
        redisTemplate.delete(redisKey);
        RedisUserSession newSession = RedisUserSession.builder().id(UUID.randomUUID().toString()).email(user.getEmail()).refreshToken(newRefreshToken).ipAddress(session.getIpAddress()).userAgent(session.getUserAgent()).expiredAt(System.currentTimeMillis() + refreshTokenExpirationMs).build();
        String newRedisKey = SESSION_PREFIX + newRefreshToken;
        redisTemplate.opsForValue().set(newRedisKey, newSession, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);
        redisTemplate.opsForSet().add(userSessionsKey, newRefreshToken);
        return JwtResponse.builder().accessToken(newAccessToken).refreshToken(newRefreshToken).build();
    }

    @Override
    public void logout(String refreshToken) {
        String redisKey = SESSION_PREFIX + refreshToken;
        RedisUserSession session = (RedisUserSession) redisTemplate.opsForValue().get(redisKey);
        if (session != null) {
            String userSessionsKey = USER_SESSIONS_PREFIX + session.getEmail();
            redisTemplate.opsForSet().remove(userSessionsKey, refreshToken);
            redisTemplate.delete(redisKey);
        }
    }

    @Override
    public UserProfileResponse getCurrentUserProfile(String userEmail) {
        return userService.getMyProfile(userEmail);
    }

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
    public List<UserSessionResponse> getAllSessions(String userEmail, String currentTokenRaw) {
        String userSessionsKey = USER_SESSIONS_PREFIX + userEmail;
        Set<Object> refreshTokens = redisTemplate.opsForSet().members(userSessionsKey);
        List<UserSessionResponse> responses = new ArrayList<>();
        if (refreshTokens != null) {
            for (Object tokenObj : refreshTokens) {
                String token = (String) tokenObj;
                String sessionKey = SESSION_PREFIX + token;
                RedisUserSession session = (RedisUserSession) redisTemplate.opsForValue().get(sessionKey);
                if (session != null) {
                    responses.add(UserSessionResponse.builder().id(session.getId()).ipAddress(session.getIpAddress()).userAgent(session.getUserAgent()).expiresAt(UserSessionResponse.mapToLocalDateTime(session.getExpiredAt())).isCurrent(false).build());
                } else {
                    redisTemplate.opsForSet().remove(userSessionsKey, token);
                }
            }
        }
        return responses;
    }

    @Override
    public void revokeSession(String sessionId, String userEmail) {
        String userSessionsKey = USER_SESSIONS_PREFIX + userEmail;
        Set<Object> refreshTokens = redisTemplate.opsForSet().members(userSessionsKey);
        if (refreshTokens != null) {
            for (Object tokenObj : refreshTokens) {
                String token = (String) tokenObj;
                String sessionKey = SESSION_PREFIX + token;
                RedisUserSession session = (RedisUserSession) redisTemplate.opsForValue().get(sessionKey);
                if (session != null && session.getId().equals(sessionId)) {
                    redisTemplate.delete(sessionKey);
                    redisTemplate.opsForSet().remove(userSessionsKey, token);
                    return;
                }
            }
        }
        throw new ResourceNotFoundException("Session not found");
    }

    @Override
    public void sendMagicLink(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Email chưa đăng ký"));
        MagicLinkToken magicToken = MagicLinkToken.create(user);
        magicLinkTokenRepository.save(magicToken);
        String link = "http://localhost:5173/magic-login?token=" + magicToken.getToken();
        EmailTask task = EmailTask.builder().toEmail(user.getEmail()).subject("Magic Link").content("Link: " + link).retryCount(0).build();
        asyncTaskProducer.submitEmailTask(task);
    }

    @Override
    public JwtResponse loginWithMagicLink(String token, HttpServletRequest request) {
        MagicLinkToken magicToken = magicLinkTokenRepository.findByToken(token).orElseThrow(() -> new BadRequestException("Link invalid"));
        if (magicToken.isExpired()) {
            magicLinkTokenRepository.delete(magicToken);
            throw new BadRequestException("Link expired");
        }
        User user = magicToken.getUser();
        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }
        magicLinkTokenRepository.delete(magicToken);
        return createTokenAndSession(user, request);
    }
    
    private void createDefaultSettings(User user) {
        // Giả sử có UserSettingsRepository hoặc service tương đương
        // Nếu không có logic này ở đây có thể bỏ qua hoặc gọi UserService
        try {
             // userService.createDefaultSettings(user); 
             // Tạm thời comment vì có thể gây vòng lặp dependency nếu inject UserService vào đây
             // Tốt nhất là xử lý sự kiện (Event) UserCreatedEvent
        } catch (Exception e) {
            log.warn("Could not create default settings for user {}", user.getId());
        }
    }
    
    private void sendActivationEmail(User user) {
        UserActivationToken token = new UserActivationToken(user);
        activationTokenRepository.save(token);
        String link = "http://localhost:5173/activate?token=" + token.getToken();
        String content = "<h1>Welcome " + user.getFullname() + "!</h1><p>Click <a href='" + link + "'>here</a> to activate.</p>";
        EmailTask task = EmailTask.builder().toEmail(user.getEmail()).subject("Welcome").content(content).retryCount(0).build();
        asyncTaskProducer.submitEmailTask(task);
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
    public void updatePasswordWithOtp(String email, String otpCode, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        OtpToken token = otpTokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Vui lòng yêu cầu gửi mã OTP trước."));

        if (token.isExpired()) {
            otpTokenRepository.delete(token);
            throw new BadRequestException("Mã OTP đã hết hạn.");
        }

        if (!token.getOtpCode().equals(otpCode)) {
            token.incrementRetry();
            otpTokenRepository.save(token);
            throw new BadRequestException("Mã OTP không chính xác.");
        }

        otpTokenRepository.delete(token);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setAuthProvider("LOCAL"); 
        userRepository.save(user);
        log.info("User {} updated password via OTP", email);
    }
}