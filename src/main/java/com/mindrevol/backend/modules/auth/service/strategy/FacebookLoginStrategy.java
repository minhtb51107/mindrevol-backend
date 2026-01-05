package com.mindrevol.backend.modules.auth.service.strategy;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.modules.auth.dto.request.FacebookLoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FacebookLoginStrategy implements SocialLoginStrategy {

    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "facebook";
    }

    @Override
    public SocialProviderData verifyAndGetData(Object data) {
        String accessToken;
        if (data instanceof FacebookLoginRequest) {
            accessToken = ((FacebookLoginRequest) data).getAccessToken();
        } else {
            accessToken = (String) data;
        }

        String url = "https://graph.facebook.com/me?fields=id,name,email,picture.width(200).height(200)&access_token=" + accessToken;
        try {
            Map<String, Object> fbProfile = restTemplate.getForObject(url, Map.class);
            if (fbProfile == null || !fbProfile.containsKey("email")) {
                throw new BadRequestException("Không lấy được email từ Facebook.");
            }

            String avatarUrl = null;
            try {
                Map<String, Object> picture = (Map<String, Object>) fbProfile.get("picture");
                Map<String, Object> picData = (Map<String, Object>) picture.get("data");
                avatarUrl = (String) picData.get("url");
            } catch (Exception ignored) {}

            return SocialProviderData.builder()
                    .providerId((String) fbProfile.get("id"))
                    .email((String) fbProfile.get("email"))
                    .name((String) fbProfile.get("name"))
                    .avatarUrl(avatarUrl)
                    .build();
        } catch (Exception e) {
            throw new BadRequestException("Facebook Auth Failed: " + e.getMessage());
        }
    }
}