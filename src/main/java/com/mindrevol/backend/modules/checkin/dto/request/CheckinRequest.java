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
    
    // Context Data (Optional)
    private String emotion;            // Emoji hoặc Mood code
    private ActivityType activityType; // Loại hoạt động
    private String activityName;       // Tên hiển thị ("Học bài", "Chill")
    private String locationName;       // Địa điểm check-in
    private List<String> tags;         // Tag bạn bè hoặc hashtag
    
    private CheckinStatus statusRequest = CheckinStatus.NORMAL; 
    private CheckinVisibility visibility = CheckinVisibility.PUBLIC;
}