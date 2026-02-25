package com.mindrevol.backend.modules.box.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BoxResponse {
    private String id;
    private String name;
    private String description;
    
    // Trang trí
    private String avatar;
    private String coverImage;
    private String themeColor;
    
    // Thông tin cơ bản
    private String ownerId;
    private Boolean isArchived;
    private long memberCount; // Số lượng người trong Box
    
    private LocalDateTime createdAt;
}