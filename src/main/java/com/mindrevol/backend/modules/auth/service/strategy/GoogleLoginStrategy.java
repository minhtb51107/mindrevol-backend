package com.mindrevol.backend.modules.auth.service.strategy;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.modules.auth.dto.request.GoogleLoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleLoginStrategy implements SocialLoginStrategy {

    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "google";
    }

    @Override
    public SocialProviderData verifyAndGetData(Object data) {
        // Data ở đây là GoogleLoginRequest hoặc String AccessToken
        String accessToken;
        if (data instanceof GoogleLoginRequest) {
            accessToken = ((GoogleLoginRequest) data).getAccessToken();
        } else {
            accessToken = (String) data;
        }

        String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + accessToken;
        try {
            Map<String, Object> googleProfile = restTemplate.getForObject(userInfoUrl, Map.class);
            if (googleProfile == null || !googleProfile.containsKey("email")) {
                throw new BadRequestException("Không thể lấy email từ Google.");
            }

            return SocialProviderData.builder()
                    .providerId((String) googleProfile.get("sub"))
                    .email((String) googleProfile.get("email"))
                    .name((String) googleProfile.get("name"))
                    .avatarUrl((String) googleProfile.get("picture"))
                    .build();
        } catch (Exception e) {
            log.error("Google verify error", e);
            throw new BadRequestException("Lỗi xác thực Google: " + e.getMessage());
        }
    }
}