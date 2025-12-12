package com.mindrevol.backend.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginRequest {
    @NotBlank(message = "Google Access Token không được để trống")
    private String accessToken; // Đã đổi tên từ idToken -> accessToken
}