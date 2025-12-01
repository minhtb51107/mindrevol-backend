package com.mindrevol.backend.modules.journey.recap.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class RecapSlide {
    private RecapSlideType type;
    private String title;           // Tiêu đề (Vd: "Ngày đầu tiên")
    private String subtitle;        // Phụ đề (Vd: "Bạn đã bắt đầu đầy hứng khởi")
    private String imageUrl;        // Ảnh check-in (nếu có)
    private LocalDate date;         // Ngày của khoảnh khắc đó
    
    // Các chỉ số phụ (dùng cho slide STATS hoặc MOST_LIKED)
    private Integer reactionCount;  
    private Integer streakCount;
}