package com.mindrevol.backend.modules.auth.service.strategy;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.modules.auth.dto.request.TikTokLoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TikTokLoginStrategy implements SocialLoginStrategy {

    private final RestTemplate restTemplate;

    @Value("${tiktok.client-key}")
    private String tiktokClientKey;

    @Value("${tiktok.client-secret}")
    private String tiktokClientSecret;

    @Override
    public String getProviderName() {
        return "tiktok";
    }

    @Override
    public SocialProviderData verifyAndGetData(Object data) {
        if (!(data instanceof TikTokLoginRequest)) {
            throw new BadRequestException("Dữ liệu đầu vào không hợp lệ cho TikTok");
        }
        TikTokLoginRequest request = (TikTokLoginRequest) data;

        try {
            // 1. Đổi Code lấy Token
            String tokenUrl = "https://open.tiktokapis.com/v2/oauth/token/";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("client_key", tiktokClientKey);
            map.add("client_secret", tiktokClientSecret);
            map.add("code", request.getCode());
            map.add("grant_type", "authorization_code");
            map.add("redirect_uri", "https://mindrevol.vercel.app/auth/callback/tiktok");
            map.add("code_verifier", request.getCodeVerifier());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
            Map<String, Object> tokenResponse = restTemplate.postForObject(tokenUrl, entity, Map.class);

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                throw new BadRequestException("Không thể lấy Access Token từ TikTok.");
            }

            String accessToken = (String) tokenResponse.get("access_token");
            String openId = (String) tokenResponse.get("open_id");

            // 2. Lấy thông tin User
            String userInfoUrl = "https://open.tiktokapis.com/v2/user/info/?fields=open_id,avatar_url,display_name";
            HttpHeaders infoHeaders = new HttpHeaders();
            infoHeaders.setBearerAuth(accessToken);
            HttpEntity<String> infoEntity = new HttpEntity<>(infoHeaders);

            ResponseEntity<Map> infoResponse = restTemplate.exchange(userInfoUrl, HttpMethod.GET, infoEntity, Map.class);

            String displayName = "TikTok User";
            String avatarUrl = null;

            if (infoResponse.getBody() != null && infoResponse.getBody().containsKey("data")) {
                Map<String, Object> dataRes = (Map<String, Object>) infoResponse.getBody().get("data");
                if (dataRes.containsKey("user")) {
                    Map<String, Object> userObj = (Map<String, Object>) dataRes.get("user");
                    displayName = (String) userObj.get("display_name");
                    avatarUrl = (String) userObj.get("avatar_url");
                }
            }

            // Fake email vì TikTok không trả về email mặc định
            String fakeEmail = "tiktok_" + openId + "@tiktok.mindrevol.com";

            return SocialProviderData.builder()
                    .providerId(openId)
                    .email(fakeEmail)
                    .name(displayName)
                    .avatarUrl(avatarUrl)
                    .build();

        } catch (Exception e) {
            log.error("TikTok Login Error", e);
            throw new BadRequestException("Lỗi xác thực TikTok: " + e.getMessage());
        }
    }
}