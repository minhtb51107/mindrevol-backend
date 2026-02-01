package com.mindrevol.backend.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckHandleRequest {
    
    @NotBlank(message = "Handle không được để trống")
    @Size(min = 3, max = 30, message = "Handle phải từ 3 đến 30 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Handle chỉ chứa chữ cái, số, dấu chấm và gạch dưới")
    private String handle;
}