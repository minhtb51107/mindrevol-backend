package com.mindrevol.backend.modules.box.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBoxRequest {
    
    @NotBlank(message = "Tên Box không được để trống")
    @Size(max = 100, message = "Tên Box không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    // Trang trí tối giản
    private String avatar;      // Link ảnh hoặc Emoji
    private String coverImage;  // Link ảnh cover
    private String themeColor;  // Mã HEX, vd: #1DA1F2
}