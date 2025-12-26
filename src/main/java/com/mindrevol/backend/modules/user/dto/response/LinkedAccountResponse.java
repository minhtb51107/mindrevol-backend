package com.mindrevol.backend.modules.user.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LinkedAccountResponse {
    private String provider; // GOOGLE, FACEBOOK, TIKTOK
    private String email;    // Email của tài khoản MXH
    private String avatarUrl;
    private boolean connected; 
}