package com.mindrevol.backend.user.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.backend.common.service.RateLimitingService;
import com.mindrevol.backend.common.utils.JwtUtil;
import com.mindrevol.backend.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.service.UserService;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private RateLimitingService rateLimitingService;

    @BeforeEach
    void setup() {
        // --- FIX LỖI NULL POINTER EXCEPTION ---
        // 1. Tạo Mock cho Bucket và Probe
        Bucket mockBucket = mock(Bucket.class);
        ConsumptionProbe mockProbe = mock(ConsumptionProbe.class);

        // 2. Cấu hình Probe: Luôn báo là còn token (isConsumed = true)
        when(mockProbe.isConsumed()).thenReturn(true);
        when(mockProbe.getRemainingTokens()).thenReturn(100L);

        // 3. Cấu hình Bucket: Khi gọi tryConsume thì trả về Probe giả ở trên
        when(mockBucket.tryConsumeAndReturnRemaining(anyLong())).thenReturn(mockProbe);

        // 4. Cấu hình Service: Trả về Bucket giả khi Filter gọi
        when(rateLimitingService.resolveGeneralBucket(anyString())).thenReturn(mockBucket);
        when(rateLimitingService.resolveLoginBucket(anyString())).thenReturn(mockBucket);
        // --------------------------------------
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void getMyProfile_ShouldReturnProfile() throws Exception {
        UserProfileResponse response = UserProfileResponse.builder()
                .email("user@test.com")
                .fullname("Test User")
                .build();

        when(userService.getMyProfile("user@test.com")).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user@test.com"))
                .andExpect(jsonPath("$.data.fullname").value("Test User"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void updateProfile_ValidationFail_ShouldReturn400() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullname("A"); 
        request.setHandle("bad handle!"); 

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getMyProfile_Unauthorized_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }
}