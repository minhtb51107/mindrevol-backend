package com.mindrevol.backend.modules.box.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBoxRequest {
    @Size(max = 100, message = "Tên Box không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    private String avatar;
    private String coverImage;
    private String themeColor;
}