package com.mindrevol.backend.modules.auth.service.strategy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SocialProviderData {
    private String providerId; // ID duy nhất từ MXH (sub, open_id...)
    private String email;
    private String name;
    private String avatarUrl;
}