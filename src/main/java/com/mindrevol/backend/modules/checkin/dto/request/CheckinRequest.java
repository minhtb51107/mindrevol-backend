package com.mindrevol.backend.modules.checkin.dto.request;

import com.mindrevol.backend.modules.checkin.entity.ActivityType;
import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.checkin.entity.CheckinVisibility;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class CheckinRequest {
    @NotNull(message = "Hành trình là bắt buộc")
    private String journeyId;

    @NotNull(message = "Ảnh check-in là bắt buộc")
    private MultipartFile file;

    private String caption;
    
    // Context Data
    private String emotion;            
    private ActivityType activityType; 
    private String activityName;       
    private String locationName;       
    
    // [THÊM MỚI] Nhận tọa độ từ Frontend gửi lên
    private Double latitude;
    private Double longitude;

    private List<String> tags;         
    
    private CheckinStatus statusRequest = CheckinStatus.NORMAL; 
    private CheckinVisibility visibility = CheckinVisibility.PUBLIC;
}