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
    
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final AsyncTaskProducer asyncTaskProducer;
    private final RedisTemplate<String, Object> redisTemplate; 
    
    private final SessionService sessionService;
    private final SocialLoginFactory socialLoginFactory;
    
    private final RestTemplate restTemplate;
    private final AppleAuthUtil appleAuthUtil;
    private final ObjectMapper objectMapper;
    
    @Value("${tiktok.client-key:}") private String tiktokClientKey;
    @Value("${tiktok.client-secret:}") private String tiktokClientSecret;

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
        
        Optional<SocialAccount> socialAccountOpt = socialAccountRepository.findByProviderAndProviderId(providerName, data.getProviderId());
        User user;
        boolean isNewUser = false; // Mặc định là user cũ

        if (socialAccountOpt.isPresent()) {
            user = socialAccountOpt.get().getUser();
        } else {
            Optional<User> existingUser = Optional.empty();
            if (data.getEmail() != null) existingUser = userRepository.findByEmail(data.getEmail());
            
            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                user = createNewSocialUser(data.getEmail(), data.getName(), data.getAvatarUrl());
                isNewUser = true; // Đánh dấu đây là user vừa được khởi tạo
            }

            SocialAccount newLink = SocialAccount.builder()
                    .user(user).provider(providerName).providerId(data.getProviderId())
                    .email(data.getEmail()).avatarUrl(data.getAvatarUrl()).build();
            socialAccountRepository.save(newLink);
        }

        JwtResponse response = sessionService.createTokenAndSession(user, servletRequest);
        response.setIsNewUser(isNewUser); // Gắn cờ vào JWT Response
        return response;
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

        String limitKey = OTP_LIMIT_PREFIX + request.getEmail();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            Long expire = redisTemplate.getExpire(limitKey, TimeUnit.SECONDS);
            throw new BadRequestException("Vui lòng đợi " + expire + " giây trước khi gửi lại mã.");
        }

        String newCode = String.format("%06d", new Random().nextInt(999999));
        String otpKey = OTP_PREFIX + request.getEmail();
        String retryKey = OTP_RETRY_PREFIX + request.getEmail();

        redisTemplate.opsForValue().set(otpKey, newCode, 5, TimeUnit.MINUTES);
        redisTemplate.delete(retryKey); 
        redisTemplate.opsForValue().set(limitKey, "1", 60, TimeUnit.SECONDS); 

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

        String cachedOtp = (String) redisTemplate.opsForValue().get(otpKey);
        if (cachedOtp == null) throw new BadRequestException("Mã OTP đã hết hạn hoặc chưa được gửi.");

        Integer retryCount = (Integer) redisTemplate.opsForValue().get(retryKey);
        if (retryCount == null) retryCount = 0;
        if (retryCount >= 5) {
            redisTemplate.delete(otpKey);
            redisTemplate.delete(retryKey);
            throw new BadRequestException("Bạn nhập sai quá 5 lần. Vui lòng yêu cầu mã mới.");
        }

        if (!cachedOtp.equals(request.getOtpCode())) {
            redisTemplate.opsForValue().increment(retryKey);
            redisTemplate.expire(retryKey, 5, TimeUnit.MINUTES);
            throw new BadRequestException("Mã OTP không chính xác. (Sai " + (retryCount + 1) + "/5 lần)");
        }

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