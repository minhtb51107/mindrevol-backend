package com.mindrevol.backend.modules.box.dto.request;

import lombok.Data;

@Data
public class CreateBoxRequest {
    private String name;
    private String description;
    private String avatar;
    private String coverImage;
    private String themeColor;
    
    // THÊM 2 TRƯỜNG NÀY
    private String textPosition;
    private String avatarPosition;
}