package com.mindrevol.backend.modules.journey.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RoadmapStatusResponse {
    private UUID taskId;
    private Integer dayNo;
    private String title;
    private String description;
    
    // Trạng thái của người đang xem (Đã làm xong task này chưa?)
    private boolean isCompleted; 
}