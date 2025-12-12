package com.mindrevol.backend.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TikTokLoginRequest {
    @NotBlank(message = "Auth Code không được để trống")
    private String code;

    @NotBlank(message = "Code Verifier không được để trống")
    private String codeVerifier; // <--- THÊM
}