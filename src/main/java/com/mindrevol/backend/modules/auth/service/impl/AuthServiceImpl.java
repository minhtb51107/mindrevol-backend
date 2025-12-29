package com.mindrevol.backend.modules.auth.service.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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
import com.mindrevol.backend.modules.auth.dto.request.*;
import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.auth.dto.response.UserSessionResponse;
import com.mindrevol.backend.modules.auth.entity.*;
import com.mindrevol.backend.modules.auth.repository.*;
import com.mindrevol.backend.modules.auth.service.AuthService;
import com.mindrevol.backend.modules.auth.util.AppleAuthUtil;
import com.mindrevol.backend.modules.notification.dto.EmailTask;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.Gender;
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
    private final OtpTokenRepository otpTokenRepository;
    private final SocialAccountRepository socialAccountRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AsyncTaskProducer asyncTaskProducer;
    private final RestTemplate restTemplate;

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

    // --- CONFIG TIKTOK ---
    @Value("${tiktok.client-key}")
    private String tiktokClientKey;

    @Value("${tiktok.client-secret}")
    private String tiktokClientSecret;

    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";

    // --- [MỚI] TIKTOK LOGIN (Đã fix thêm code_verifier) ---
    @Override
    public JwtResponse loginWithTikTok(TikTokLoginRequest request, HttpServletRequest servletRequest) {
        try {
            // B1: Đổi Code lấy Access Token
            String tokenUrl = "https://open.tiktokapis.com/v2/oauth/token/";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("client_key", tiktokClientKey);
            map.add("client_secret", tiktokClientSecret);
            map.add("code", request.getCode());
            map.add("grant_type", "authorization_code");
            map.add("redirect_uri", "https://mindrevol.vercel.app/auth/callback/tiktok");
            
            // --- QUAN TRỌNG: Gửi Verifier để TikTok kiểm tra khớp với Challenge lúc nãy ---
            map.add("code_verifier", request.getCodeVerifier());
            // ----------------------------------------------------------------------------

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
            
            Map<String, Object> tokenResponse = restTemplate.postForObject(tokenUrl, entity, Map.class);
            
            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                throw new BadRequestException("Không thể lấy Access Token từ TikTok. (Token Response: " + tokenResponse + ")");
            }
            
            String accessToken = (String) tokenResponse.get("access_token");
            String openId = (String) tokenResponse.get("open_id");

            // B2: Lấy thông tin User
            String userInfoUrl = "https://open.tiktokapis.com/v2/user/info/?fields=open_id,union_id,avatar_url,display_name";
            HttpHeaders infoHeaders = new HttpHeaders();
            infoHeaders.setBearerAuth(accessToken);
            HttpEntity<String> infoEntity = new HttpEntity<>(infoHeaders);
            
            ResponseEntity<Map> infoResponse = restTemplate.exchange(userInfoUrl, HttpMethod.GET, infoEntity, Map.class);
            
            String displayName = "TikTok User";
            String avatarUrl = null;
            
            if (infoResponse.getBody() != null && infoResponse.getBody().containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) infoResponse.getBody().get("data");
                if (data.containsKey("user")) {
                    Map<String, Object> userObj = (Map<String, Object>) data.get("user");
                    displayName = (String) userObj.get("display_name");
                    avatarUrl = (String) userObj.get("avatar_url");
                }
            }

            // Tạo email giả định để định danh
            String fakeEmail = "tiktok_" + openId + "@tiktok.mindrevol.com";

            // B3: Xử lý đăng nhập
            User user = processSocialLogin(fakeEmail, displayName, avatarUrl, "TIKTOK", openId);
            
            return createTokenAndSession(user, servletRequest);

        } catch (Exception e) {
            log.error("TikTok Login Error", e);
            throw new BadRequestException("Lỗi xác thực TikTok: " + e.getMessage());
        }
    }

    // --- CÁC HÀM KHÁC (Giữ nguyên) ---

    @Override
    public void registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng.");
        }
        if (userRepository.existsByHandle(request.getHandle())) {
            throw new BadRequestException("Handle @" + request.getHandle() + " đã tồn tại.");
        }
        if (request.getDateOfBirth() == null) throw new BadRequestException("Ngày sinh là bắt buộc.");
        
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").description("Default user").build()));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullname(request.getFullname())
                .handle(request.getHandle())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender()) 
                .status(UserStatus.PENDING_ACTIVATION)
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Lỗi dữ liệu: Tên người dùng hoặc Email đã tồn tại.");
        }
        sendActivationEmail(user);
    }

    @Override
    @Transactional
    public void sendOtpLogin(SendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email này chưa đăng ký tài khoản."));
        OtpToken otpToken = otpTokenRepository.findByUserId(user.getId())
                .orElseGet(() -> OtpToken.builder().user(user).build());
        String newCode = String.format("%06d", new Random().nextInt(999999));
        otpToken.setOtpCode(newCode);
        otpToken.setRetryCount(0);
        otpToken.setExpiresAt(java.time.OffsetDateTime.now().plusMinutes(5));
        otpTokenRepository.save(otpToken);
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

    private User processSocialLogin(String email, String name, String avatarUrl, String provider, String providerId) {
        Optional<SocialAccount> socialAccountOpt = socialAccountRepository.findByProviderAndProviderId(provider, providerId);
        if (socialAccountOpt.isPresent()) {
            return socialAccountOpt.get().getUser(); 
        }
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createNewSocialUser(email, name, avatarUrl)); 

        SocialAccount newLink = SocialAccount.builder()
                .user(user)
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .avatarUrl(avatarUrl)
                .build();
        socialAccountRepository.save(newLink);
        return user;
    }

    private User createNewSocialUser(String email, String name, String avatarUrl) {
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
                .avatarUrl(avatarUrl)
                .handle(handle)
                .password(passwordEncoder.encode(UUID.randomUUID().toString())) 
                .status(UserStatus.ACTIVE)
                .gender(Gender.PREFER_NOT_TO_SAY) 
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();
        return userRepository.save(newUser);
    }

    @Override
    public JwtResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest servletRequest) {
        String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + request.getAccessToken();
        try {
            Map<String, Object> googleProfile = restTemplate.getForObject(userInfoUrl, Map.class);
            if (googleProfile == null || !googleProfile.containsKey("email")) throw new BadRequestException("Không thể lấy email từ Google.");
            String email = (String) googleProfile.get("email");
            String sub = (String) googleProfile.get("sub"); 
            String name = (String) googleProfile.get("name");
            String picture = (String) googleProfile.get("picture");
            User user = processSocialLogin(email, name, picture, "GOOGLE", sub);
            return createTokenAndSession(user, servletRequest);
        } catch (Exception e) {
            throw new BadRequestException("Lỗi xác thực Google: " + e.getMessage());
        }
    }

    @Override
    public JwtResponse loginWithFacebook(FacebookLoginRequest request, HttpServletRequest servletRequest) {
        String url = "https://graph.facebook.com/me?fields=id,name,email,picture.width(200).height(200)&access_token=" + request.getAccessToken();
        try {
            Map<String, Object> fbProfile = restTemplate.getForObject(url, Map.class);
            if (fbProfile == null || !fbProfile.containsKey("email")) throw new BadRequestException("Không lấy được email từ Facebook.");
            String email = (String) fbProfile.get("email");
            String fbId = (String) fbProfile.get("id");
            String name = (String) fbProfile.get("name");
            String avatarUrl = null;
            try {
                Map<String, Object> picture = (Map<String, Object>) fbProfile.get("picture");
                Map<String, Object> data = (Map<String, Object>) picture.get("data");
                avatarUrl = (String) data.get("url");
            } catch (Exception ignored) {}
            User user = processSocialLogin(email, name, avatarUrl, "FACEBOOK", fbId);
            return createTokenAndSession(user, servletRequest);
        } catch (Exception e) {
            throw new BadRequestException("Facebook Auth Failed: " + e.getMessage());
        }
    }

    @Override
    public JwtResponse loginWithApple(AppleLoginRequest request, HttpServletRequest servletRequest) {
        try {
            Claims claims = appleAuthUtil.validateToken(request.getIdentityToken());
            String email = claims.get("email", String.class);
            String sub = claims.getSubject();
            if (email == null) throw new BadRequestException("Apple không trả về email.");
            String name = "Apple User"; 
            if (request.getUser() != null) {
                try {
                    JsonNode node = objectMapper.readTree(request.getUser());
                    JsonNode nameNode = node.get("name");
                    if (nameNode != null) {
                        String fName = nameNode.has("firstName") ? nameNode.get("firstName").asText() : "";
                        String lName = nameNode.has("lastName") ? nameNode.get("lastName").asText() : "";
                        name = (fName + " " + lName).trim();
                    }
                } catch (Exception ignored) {}
            }
            User user = processSocialLogin(email, name, null, "APPLE", sub);
            return createTokenAndSession(user, servletRequest);
        } catch (Exception e) {
            throw new BadRequestException("Apple Auth Failed: " + e.getMessage());
        }
    }

    @Override
    public UserSummaryResponse checkEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return UserSummaryResponse.builder().id(user.getId()).fullname(user.getFullname()).handle(user.getHandle()).avatarUrl(user.getAvatarUrl()).isOnline(true).build();
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
        // Nếu AuthProvider là LOCAL thì chắc chắn có password
        // Nếu là Google/Facebook... thì mặc định ban đầu là chưa có password (password rác)
        return "LOCAL".equals(user.getAuthProvider());
    }

    @Override
    public void createPassword(CreatePasswordRequest request, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        
        // Chuyển trạng thái thành LOCAL để hệ thống nhận diện là User này đã có password
        // Điều này cho phép họ sử dụng cả đăng nhập Google VÀ đăng nhập bằng Mật khẩu
        user.setAuthProvider("LOCAL"); 
        
        userRepository.save(user);
    }
}