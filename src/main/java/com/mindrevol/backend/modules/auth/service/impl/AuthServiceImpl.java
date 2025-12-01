package com.mindrevol.backend.modules.auth.service.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.common.service.AsyncTaskProducer;
import com.mindrevol.backend.common.utils.JwtUtil;
import com.mindrevol.backend.modules.auth.dto.RedisUserSession;
import com.mindrevol.backend.modules.auth.dto.request.AppleLoginRequest;
import com.mindrevol.backend.modules.auth.dto.request.ChangePasswordRequest;
import com.mindrevol.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.mindrevol.backend.modules.auth.dto.request.LoginRequest;
import com.mindrevol.backend.modules.auth.dto.request.RegisterRequest;
import com.mindrevol.backend.modules.auth.dto.request.ResetPasswordRequest;
import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.auth.dto.response.UserSessionResponse;
import com.mindrevol.backend.modules.auth.entity.MagicLinkToken;
import com.mindrevol.backend.modules.auth.entity.PasswordResetToken;
import com.mindrevol.backend.modules.auth.entity.UserActivationToken;
import com.mindrevol.backend.modules.auth.repository.MagicLinkTokenRepository;
import com.mindrevol.backend.modules.auth.repository.PasswordResetTokenRepository;
import com.mindrevol.backend.modules.auth.repository.UserActivationTokenRepository;
import com.mindrevol.backend.modules.auth.service.AuthService;
import com.mindrevol.backend.modules.auth.util.AppleAuthUtil;
import com.mindrevol.backend.modules.notification.dto.EmailTask;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.entity.Role;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.entity.UserStatus;
import com.mindrevol.backend.modules.user.repository.RoleRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.user.service.UserService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    // private final EmailService emailService; // Đã loại bỏ vì dùng Queue
    private final AsyncTaskProducer asyncTaskProducer; // Inject Producer để đẩy task vào Queue

    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AppleAuthUtil appleAuthUtil;
    private final ObjectMapper objectMapper;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String appleClientId;

    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";
    
    @Override
    public void registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng.");
        }
        if (userRepository.existsByHandle(request.getHandle())) {
            throw new BadRequestException("Handle @" + request.getHandle() + " đã tồn tại.");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").description("Default user").build()));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullname(request.getFullname())
                .handle(request.getHandle())
                .status(UserStatus.PENDING_ACTIVATION)
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .authProvider("LOCAL") 
                .build();

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Tên người dùng hoặc Email đã tồn tại. Vui lòng thử lại.");
        }

        // Gửi email kích hoạt qua Queue
        sendActivationEmail(user);
    }

    @Override
    public void activateUserAccount(String token) {
        UserActivationToken activationToken = activationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Token không hợp lệ."));

        if (activationToken.isExpired()) {
            activationTokenRepository.delete(activationToken);
            throw new BadRequestException("Token đã hết hạn.");
        }

        User user = activationToken.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        activationTokenRepository.delete(activationToken);
    }

    @Override
    public JwtResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("Tài khoản chưa kích hoạt hoặc bị khóa.");
        }

        return createTokenAndSession(user, servletRequest);
    }

    @Override
    public JwtResponse loginWithGoogle(String idTokenString, HttpServletRequest servletRequest) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) throw new BadRequestException("Invalid Google Token");

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> registerNewUserFromGoogle(payload));

            return createTokenAndSession(user, servletRequest);

        } catch (GeneralSecurityException | IOException e) {
            throw new BadRequestException("Google Auth Failed: " + e.getMessage());
        }
    }

    private User registerNewUserFromGoogle(GoogleIdToken.Payload payload) {
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");
        
        String baseHandle = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        String handle = baseHandle;
        int suffix = 1;
        while (userRepository.existsByHandle(handle)) {
            handle = baseHandle + "." + (++suffix);
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));

        User newUser = User.builder()
                .email(email)
                .fullname(name)
                .avatarUrl(picture)
                .handle(handle)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .authProvider("GOOGLE") 
                .build();

        return userRepository.save(newUser);
    }

    private JwtResponse createTokenAndSession(User user, HttpServletRequest request) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        RedisUserSession redisSession = RedisUserSession.builder()
                .id(UUID.randomUUID().toString())
                .email(user.getEmail())
                .refreshToken(refreshToken)
                .ipAddress(request.getRemoteAddr())
                .userAgent(request.getHeader("User-Agent"))
                .expiredAt(System.currentTimeMillis() + refreshTokenExpirationMs)
                .build();

        String redisKey = SESSION_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(redisKey, redisSession, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);

        String userSessionsKey = USER_SESSIONS_PREFIX + user.getEmail();
        redisTemplate.opsForSet().add(userSessionsKey, refreshToken);
        redisTemplate.expire(userSessionsKey, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);

        log.info("Saved session for user: {}", user.getEmail());

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public JwtResponse refreshToken(String refreshToken) {
        String redisKey = SESSION_PREFIX + refreshToken;
        RedisUserSession session = (RedisUserSession) redisTemplate.opsForValue().get(redisKey);
        
        if (session == null) {
            throw new BadRequestException("Invalid Refresh Token");
        }
        User user = userRepository.findByEmail(session.getEmail()).orElseThrow();

        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);

        String userSessionsKey = USER_SESSIONS_PREFIX + user.getEmail();
        redisTemplate.opsForSet().remove(userSessionsKey, refreshToken);
        redisTemplate.delete(redisKey);

        RedisUserSession newSession = RedisUserSession.builder()
                .id(UUID.randomUUID().toString())
                .email(user.getEmail())
                .refreshToken(newRefreshToken)
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .expiredAt(System.currentTimeMillis() + refreshTokenExpirationMs)
                .build();

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
            log.info("Logged out session: {}", session.getId());
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
            
            // Đẩy task gửi mail reset password vào Queue
            EmailTask task = EmailTask.builder()
                    .toEmail(user.getEmail())
                    .subject("Reset Password")
                    .content("Click here to reset password: " + link)
                    .retryCount(0)
                    .build();
            
            asyncTaskProducer.submitEmailTask(task);
        });
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid Token"));
        
        if (token.isExpired()) throw new BadRequestException("Expired Token");

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.delete(token);
    }

    @Override
    public void changePassword(ChangePasswordRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        if (!"LOCAL".equals(user.getAuthProvider())) {
            throw new BadRequestException("Tài khoản này đăng nhập bằng Google/Apple, không thể đổi mật khẩu.");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Wrong old password");
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
                    boolean isCurrent = false; 
                    responses.add(UserSessionResponse.builder()
                            .id(session.getId())
                            .ipAddress(session.getIpAddress())
                            .userAgent(session.getUserAgent())
                            .expiresAt(UserSessionResponse.mapToLocalDateTime(session.getExpiredAt()))
                            .isCurrent(isCurrent)
                            .build());
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
                    log.info("Revoked session {} for user {}", sessionId, userEmail);
                    return;
                }
            }
        }
        throw new ResourceNotFoundException("Không tìm thấy phiên đăng nhập.");
    }
    
    @Override
    public void sendMagicLink(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email chưa được đăng ký."));

        MagicLinkToken magicToken = MagicLinkToken.create(user);
        magicLinkTokenRepository.save(magicToken);

        String link = "http://localhost:5173/magic-login?token=" + magicToken.getToken();
        String body = "<h1>Đăng nhập không cần mật khẩu</h1>" +
                      "<p>Click link sau: <a href=\"" + link + "\">Đăng nhập ngay</a></p>";
        
        // Đẩy task gửi Magic Link vào Queue
        EmailTask task = EmailTask.builder()
                .toEmail(user.getEmail())
                .subject("Magic Link Login")
                .content(body)
                .retryCount(0)
                .build();
        
        asyncTaskProducer.submitEmailTask(task);
    }

    @Override
    public JwtResponse loginWithMagicLink(String token, HttpServletRequest request) {
        MagicLinkToken magicToken = magicLinkTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Link đăng nhập không hợp lệ."));

        if (magicToken.isExpired()) {
            magicLinkTokenRepository.delete(magicToken);
            throw new BadRequestException("Link đăng nhập đã hết hạn.");
        }

        User user = magicToken.getUser();
        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }

        magicLinkTokenRepository.delete(magicToken);
        return createTokenAndSession(user, request);
    }
    
    @Override
    public JwtResponse loginWithApple(AppleLoginRequest request, HttpServletRequest servletRequest) {
        try {
            Claims claims = appleAuthUtil.validateToken(request.getIdentityToken());
            
            if (!claims.getAudience().equals(appleClientId)) {
                throw new BadRequestException("Token Audience không khớp.");
            }

            String email = claims.get("email", String.class);
            String sub = claims.getSubject();

            if (email == null) {
                throw new BadRequestException("Apple không cung cấp email.");
            }

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> registerNewUserFromApple(email, sub, request.getUser()));

            return createTokenAndSession(user, servletRequest);

        } catch (Exception e) {
            throw new BadRequestException("Lỗi xác thực Apple: " + e.getMessage());
        }
    }

    private User registerNewUserFromApple(String email, String sub, String userJson) {
        String fullname = "Apple User";
        
        if (userJson != null) {
            try {
                JsonNode node = objectMapper.readTree(userJson);
                JsonNode nameNode = node.get("name");
                if (nameNode != null) {
                    String firstName = nameNode.has("firstName") ? nameNode.get("firstName").asText() : "";
                    String lastName = nameNode.has("lastName") ? nameNode.get("lastName").asText() : "";
                    fullname = (firstName + " " + lastName).trim();
                }
            } catch (Exception e) {
                log.warn("Không parse được tên từ Apple: {}", e.getMessage());
            }
        }

        String baseHandle = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        String handle = baseHandle;
        int suffix = 1;
        while (userRepository.existsByHandle(handle)) {
            handle = baseHandle + "." + (++suffix);
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));

        User newUser = User.builder()
                .email(email)
                .fullname(fullname.isEmpty() ? "Apple User" : fullname)
                .handle(handle)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .authProvider("APPLE")
                .build();

        return userRepository.save(newUser);
    }

    private void sendActivationEmail(User user) {
        UserActivationToken token = new UserActivationToken(user);
        activationTokenRepository.save(token);
        String link = "http://localhost:5173/activate?token=" + token.getToken();
        String content = "<h1>Welcome " + user.getFullname() + "!</h1>" +
                         "<p>Click <a href='" + link + "'>here</a> to activate.</p>";

        // Đẩy task gửi mail kích hoạt vào Queue
        EmailTask task = EmailTask.builder()
                .toEmail(user.getEmail())
                .subject("Welcome to MindRevol")
                .content(content)
                .retryCount(0)
                .build();

        asyncTaskProducer.submitEmailTask(task);
    }
}