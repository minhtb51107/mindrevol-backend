package com.mindrevol.backend.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppleLoginRequest {
    @NotBlank(message = "Identity Token không được để trống")
    private String identityToken;

    private String user; 
}