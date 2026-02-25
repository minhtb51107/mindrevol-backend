package com.mindrevol.backend.modules.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.common.service.AsyncTaskProducer;
import com.mindrevol.backend.modules.auth.dto.request.*;
import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.auth.entity.MagicLinkToken;
import com.mindrevol.backend.modules.auth.entity.SocialAccount;
import com.mindrevol.backend.modules.auth.repository.MagicLinkTokenRepository;
import com.mindrevol.backend.modules.auth.repository.SocialAccountRepository;
import com.mindrevol.backend.modules.auth.service.AuthService;
import com.mindrevol.backend.modules.auth.service.SessionService;
import com.mindrevol.backend.modules.auth.service.strategy.SocialLoginFactory;
import com.mindrevol.backend.modules.auth.service.strategy.SocialLoginStrategy;
import com.mindrevol.backend.modules.auth.service.strategy.SocialProviderData;
import com.mindrevol.backend.modules.auth.util.AppleAuthUtil;
import com.mindrevol.backend.modules.notification.dto.EmailTask;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.entity.*;
import com.mindrevol.backend.modules.user.repository.RoleRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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
    private final SocialAccountRepository socialAccountRepository;
    private final MagicLinkTokenRepository magicLinkTokenRepository;
    // ĐÃ XÓA OtpTokenRepository
    
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final AsyncTaskProducer asyncTaskProducer;
    private final RedisTemplate<String, Object> redisTemplate; // Inject RedisTemplate
    
    // Services tách ra
    private final SessionService sessionService;
    private final SocialLoginFactory socialLoginFactory;
    
    // Inject các bean cũ
    private final RestTemplate restTemplate;
    private final AppleAuthUtil appleAuthUtil;
    private final ObjectMapper objectMapper;
    
    @Value("${tiktok.client-key:}") private String tiktokClientKey;
    @Value("${tiktok.client-secret:}") private String tiktokClientSecret;

    // Hằng số Key Redis
    private static final String OTP_PREFIX = "auth:otp:code:";       
    private static final String OTP_LIMIT_PREFIX = "auth:otp:limit:"; 
    private static final String OTP_RETRY_PREFIX = "auth:otp:retry:"; 

    @Override
    public JwtResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) throw new DisabledException("Tài khoản bị khóa.");
        return sessionService.createTokenAndSession(user, servletRequest);
    }

    // ==================================================================================
    // SOCIAL LOGIN LOGIC
    // ==================================================================================

    private JwtResponse processUnifiedSocialLogin(String providerName, Object requestData, HttpServletRequest servletRequest) {
        SocialLoginStrategy strategy = socialLoginFactory.getStrategy(providerName);
        SocialProviderData data = strategy.verifyAndGetData(requestData);
        User user = findOrCreateUser(providerName, data);
        return sessionService.createTokenAndSession(user, servletRequest);
    }

    private User findOrCreateUser(String provider, SocialProviderData data) {
        Optional<SocialAccount> socialAccountOpt = socialAccountRepository.findByProviderAndProviderId(provider, data.getProviderId());
        if (socialAccountOpt.isPresent()) return socialAccountOpt.get().getUser();

        User user;
        Optional<User> existingUser = Optional.empty();
        if (data.getEmail() != null) existingUser = userRepository.findByEmail(data.getEmail());
        
        if (existingUser.isPresent()) user = existingUser.get();
        else user = createNewSocialUser(data.getEmail(), data.getName(), data.getAvatarUrl());

        SocialAccount newLink = SocialAccount.builder()
                .user(user).provider(provider).providerId(data.getProviderId())
                .email(data.getEmail()).avatarUrl(data.getAvatarUrl()).build();
        socialAccountRepository.save(newLink);
        return user;
    }

    private User createNewSocialUser(String email, String name, String avatarUrl) {
        String safeEmail = (email != null) ? email : "no-email-" + UUID.randomUUID() + "@mindrevol.local";
        String baseHandle = safeEmail.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        if (baseHandle.isEmpty()) baseHandle = "user";
        
        String handle = baseHandle;
        int suffix = 1;
        while (userRepository.existsByHandle(handle)) handle = baseHandle + "." + (++suffix);
        
        Role userRole = roleRepository.findByName("USER").orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));
        
        User newUser = User.builder()
                .email(safeEmail).fullname(name != null ? name : "New User")
                .avatarUrl(avatarUrl).handle(handle)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .status(UserStatus.ACTIVE).gender(Gender.PREFER_NOT_TO_SAY)
                .accountType(AccountType.FREE).authProvider("SOCIAL")
                .roles(new HashSet<>(Collections.singletonList(userRole))).build();
        return userRepository.save(newUser);
    }

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
    // OTP REDIS IMPLEMENTATION
    // ==================================================================================

    @Override
    public void sendOtpLogin(SendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email này chưa đăng ký tài khoản."));

        // 1. Kiểm tra Rate Limit (60s)
        String limitKey = OTP_LIMIT_PREFIX + request.getEmail();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            Long expire = redisTemplate.getExpire(limitKey, TimeUnit.SECONDS);
            throw new BadRequestException("Vui lòng đợi " + expire + " giây trước khi gửi lại mã.");
        }

        // 2. Sinh mã & Key
        String newCode = String.format("%06d", new Random().nextInt(999999));
        String otpKey = OTP_PREFIX + request.getEmail();
        String retryKey = OTP_RETRY_PREFIX + request.getEmail();

        // 3. Lưu vào Redis
        redisTemplate.opsForValue().set(otpKey, newCode, 5, TimeUnit.MINUTES);
        redisTemplate.delete(retryKey); // Reset retry count
        redisTemplate.opsForValue().set(limitKey, "1", 60, TimeUnit.SECONDS); // Set Rate Limit

        // 4. Gửi Email (Async)
        String subject = "Mã xác thực đăng nhập MindRevol";
        String content = "<h1>Mã OTP của bạn: " + newCode + "</h1><p>Hết hạn sau 5 phút.</p>";
        EmailTask task = EmailTask.builder().toEmail(user.getEmail()).subject(subject).content(content).retryCount(0).build();
        asyncTaskProducer.submitEmailTask(task);
        
        log.info("Sent OTP login to user: {}", user.getEmail());
    }

    @Override
    public JwtResponse verifyOtpLogin(VerifyOtpRequest request, HttpServletRequest servletRequest) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String otpKey = OTP_PREFIX + request.getEmail();
        String retryKey = OTP_RETRY_PREFIX + request.getEmail();

        // 1. Kiểm tra OTP tồn tại
        String cachedOtp = (String) redisTemplate.opsForValue().get(otpKey);
        if (cachedOtp == null) throw new BadRequestException("Mã OTP đã hết hạn hoặc chưa được gửi.");

        // 2. Kiểm tra số lần sai
        Integer retryCount = (Integer) redisTemplate.opsForValue().get(retryKey);
        if (retryCount == null) retryCount = 0;
        if (retryCount >= 5) {
            redisTemplate.delete(otpKey);
            redisTemplate.delete(retryKey);
            throw new BadRequestException("Bạn nhập sai quá 5 lần. Vui lòng yêu cầu mã mới.");
        }

        // 3. So sánh
        if (!cachedOtp.equals(request.getOtpCode())) {
            redisTemplate.opsForValue().increment(retryKey);
            redisTemplate.expire(retryKey, 5, TimeUnit.MINUTES);
            throw new BadRequestException("Mã OTP không chính xác. (Sai " + (retryCount + 1) + "/5 lần)");
        }

        // 4. Thành công -> Dọn dẹp
        redisTemplate.delete(otpKey);
        redisTemplate.delete(retryKey);
        redisTemplate.delete(OTP_LIMIT_PREFIX + request.getEmail());

        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }
        return sessionService.createTokenAndSession(user, servletRequest);
    }

    // ==================================================================================
    // MAGIC LINK & PROFILE
    // ==================================================================================

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
        return sessionService.createTokenAndSession(user, request);
    }

    @Override
    public UserProfileResponse getCurrentUserProfile(String userEmail) {
        return userService.getMyProfile(userEmail);
    }
}