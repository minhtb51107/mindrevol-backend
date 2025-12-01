package com.mindrevol.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.utils.JwtUtil;
import com.mindrevol.backend.modules.auth.dto.RedisUserSession;
import com.mindrevol.backend.modules.auth.dto.request.LoginRequest;
import com.mindrevol.backend.modules.auth.dto.request.RegisterRequest;
import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.auth.entity.UserActivationToken;
import com.mindrevol.backend.modules.auth.repository.UserActivationTokenRepository;
import com.mindrevol.backend.modules.auth.service.impl.AuthServiceImpl;
import com.mindrevol.backend.modules.notification.service.EmailService;
import com.mindrevol.backend.modules.user.entity.Role;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.entity.UserStatus;
import com.mindrevol.backend.modules.user.repository.RoleRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserActivationTokenRepository activationTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private EmailService emailService;
    
    // --- MOCK REDIS ---
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private SetOperations<String, Object> setOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // Inject giá trị cấu hình vào service
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", 3600000L);
    }

    @Test
    void registerUser_Success() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@test.com");
        req.setPassword("password");
        req.setFullname("New User");
        req.setHandle("newuser");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByHandle(anyString())).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(new Role()));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        
        authService.registerUser(req);

        verify(userRepository).save(any(User.class));
        verify(activationTokenRepository).save(any(UserActivationToken.class));
        verify(emailService).sendEmail(eq(req.getEmail()), anyString(), anyString());
    }

    @Test
    void login_Success_ShouldSaveToRedis() {
        // Arrange
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("user@test.com");
        loginReq.setPassword("pass");

        User user = User.builder().email("user@test.com").status(UserStatus.ACTIVE).build();
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);

        when(authenticationManager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken("user", "pass"));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(user)).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(user)).thenReturn("refresh_token");

        // Mock Redis Calls
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        // Act
        JwtResponse response = authService.login(loginReq, servletRequest);

        // Assert
        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        
        // Kiểm tra Redis đã được gọi để lưu session chưa
        verify(valueOperations).set(contains("session:"), any(RedisUserSession.class), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(setOperations).add(contains("user_sessions:"), anyString());
    }
    
    @Test
    void registerUser_DuplicateEmail_ThrowsException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("exist@test.com");
        
        when(userRepository.existsByEmail("exist@test.com")).thenReturn(true);
        
        assertThrows(BadRequestException.class, () -> authService.registerUser(req));
    }
}