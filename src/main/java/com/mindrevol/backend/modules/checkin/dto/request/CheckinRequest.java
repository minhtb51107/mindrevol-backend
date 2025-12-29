package com.mindrevol.backend.modules.checkin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.checkin.entity.CheckinVisibility;

@Data
public class CheckinRequest {
    @NotNull(message = "Hành trình là bắt buộc")
    private Long journeyId; // [FIX] Đổi UUID -> Long

    @NotNull(message = "Ảnh check-in là bắt buộc")
    private MultipartFile file;

    @NotNull(message = "Cảm xúc là bắt buộc")
    private String emotion;

    private String caption;
    
    private CheckinStatus statusRequest = CheckinStatus.NORMAL; 
    
    // [ĐÃ XÓA] private UUID taskId;

    private CheckinVisibility visibility = CheckinVisibility.PUBLIC;
}