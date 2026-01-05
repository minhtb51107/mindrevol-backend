package com.mindrevol.backend.common.service;

import com.mindrevol.backend.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Service
@Slf4j
public class ContentModerationService {

    @Value("${app.moderation.api-user:}") 
    private String apiUser;

    @Value("${app.moderation.api-secret:}")
    private String apiSecret;

    @Value("${app.moderation.enabled:false}")
    private boolean isEnabled;

    private final RestTemplate restTemplate = new RestTemplate();

    public void validateImage(String imageUrl) {
        if (!isEnabled || apiUser.isEmpty()) {
            log.debug("Content moderation is disabled or missing keys.");
            return;
        }

        try {
            // Gọi API Sightengine để check Nudity, Weapon, Alcohol, Drugs
            String apiUrl = String.format(
                "https://api.sightengine.com/1.0/check.json?models=nudity,wad,offensive&api_user=%s&api_secret=%s&url=%s",
                apiUser, apiSecret, imageUrl
            );

            ResponseEntity<Map> response = restTemplate.getForEntity(apiUrl, Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null && "success".equals(body.get("status"))) {
                // Parse kết quả (Ví dụ đơn giản)
                // Trong thực tế cần parse kỹ field 'nudity.safe', 'weapon', v.v.
                
                // Logic giả định: Nếu API trả về xác suất vi phạm cao > 0.8
                // Check 'nudity'
                Map<String, Object> nudity = (Map<String, Object>) body.get("nudity");
                if (nudity != null) {
                    Double safe = (Double) nudity.get("safe");
                    if (safe != null && safe < 0.15) { // Dưới 15% an toàn
                        throw new BadRequestException("Ảnh chứa nội dung nhạy cảm (Khỏa thân).");
                    }
                }
                
                // Check 'offensive'
                Map<String, Object> offensive = (Map<String, Object>) body.get("offensive");
                if (offensive != null) {
                    Double prob = (Double) offensive.get("prob");
                    if (prob != null && prob > 0.8) {
                        throw new BadRequestException("Ảnh chứa nội dung xúc phạm/nhạy cảm.");
                    }
                }
                
                log.info("Image passed moderation check: {}", imageUrl);
            }

        } catch (BadRequestException e) {
            throw e; // Ném tiếp lỗi ra ngoài
        } catch (Exception e) {
            log.error("Moderation API error", e);
            // Tùy chính sách: Nếu API lỗi thì cho qua hay chặn?
            // Ở đây tạm thời cho qua (Fail-open)
        }
    }
}