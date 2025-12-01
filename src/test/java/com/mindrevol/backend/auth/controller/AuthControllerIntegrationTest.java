package com.mindrevol.backend.auth.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print; // Import quan trọng để debug
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.backend.common.service.RateLimitingService;
import com.mindrevol.backend.modules.auth.dto.request.LoginRequest;
import com.mindrevol.backend.modules.auth.dto.request.RegisterRequest;
import com.mindrevol.backend.modules.user.entity.Role;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.entity.UserStatus;
import com.mindrevol.backend.modules.user.repository.RoleRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    // --- MOCK TOÀN BỘ REDIS (Blocking & Reactive) ---
    
    @MockBean private RateLimitingService rateLimitingService;
    @MockBean private RedisTemplate<String, Object> redisTemplate;
    @MockBean private RedisConnectionFactory redisConnectionFactory;
    
    // Fix lỗi "ReactiveRedisConnectionFactory" mà bạn gặp lúc nãy
    @MockBean private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory; 

    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private SetOperations<String, Object> setOperations;

    @BeforeEach
    void setUp() {
        // 1. Mock hành vi Redis Template (để AuthService không bị NullPointer khi gọi)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        // 2. Mock Rate Limit (Luôn cho phép login)
        Bucket mockBucket = mock(Bucket.class);
        ConsumptionProbe mockProbe = mock(ConsumptionProbe.class);
        when(mockProbe.isConsumed()).thenReturn(true);
        when(mockProbe.getRemainingTokens()).thenReturn(100L);
        when(mockBucket.tryConsumeAndReturnRemaining(anyLong())).thenReturn(mockProbe);
        
        when(rateLimitingService.resolveLoginBucket(anyString())).thenReturn(mockBucket);
        when(rateLimitingService.resolveGeneralBucket(anyString())).thenReturn(mockBucket);

        // 3. Setup Dữ liệu DB
        userRepository.deleteAll();
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));

        User user = User.builder()
                .email("user@example.com")
                .handle("user_test")
                .fullname("User Test")
                .password(passwordEncoder.encode("password123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .authProvider("LOCAL")
                .build();
        userRepository.save(user);
    }

    @Test
    @DisplayName("API Login: Thành công trả về 200 và Token")
    void login_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print()) // <--- QUAN TRỌNG: In lỗi ra console nếu fail
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    @DisplayName("API Login: Thất bại khi sai mật khẩu")
    void login_Fail_WrongPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("wrong_password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không chính xác."));
    }

    @Test
    @DisplayName("API Register: Thành công trả về 201")
    void register_Success() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setFullname("New User");
        registerRequest.setHandle("new_user");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Đăng ký thành công! Vui lòng kiểm tra email để kích hoạt."));
    }
}