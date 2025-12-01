package com.mindrevol.backend.modules.journey.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JourneyWidgetResponse {
    private String journeyName;
    private int currentStreak;
    private boolean isCompletedToday;
    private String latestThumbnailUrl;
    
    // --- THÊM MỚI ---
    private WidgetStatus status; // Enum để App chọn màu/icon
    private String statusLabel;  // Text hiển thị (Vd: "Cố lên!", "Đã xong", "Mất chuỗi rồi")
    
    // Thêm thông tin người check-in (để sau này hiện avatar bạn bè nếu cần)
    private String ownerName;
    private String ownerAvatar;
}