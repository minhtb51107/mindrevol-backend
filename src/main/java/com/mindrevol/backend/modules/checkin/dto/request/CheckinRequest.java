package com.mindrevol.backend.modules.checkin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.checkin.entity.CheckinVisibility;
//import com.mindrevol.backend.modules.checkin.entity.Emotion;

import java.util.UUID;

@Data
public class CheckinRequest {
    @NotNull(message = "Hành trình là bắt buộc")
    private UUID journeyId;

    @NotNull(message = "Ảnh check-in là bắt buộc")
    private MultipartFile file; // File ảnh từ Client gửi lên

    @NotNull(message = "Cảm xúc là bắt buộc")
    private String emotion;

    private String caption;
    
    private CheckinStatus statusRequest = CheckinStatus.NORMAL; 
    
    private UUID taskId;

    // Default là PUBLIC nếu không gửi lên
    private CheckinVisibility visibility = CheckinVisibility.PUBLIC;
}