package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class UserActiveJourneyResponse {
    private String id;
    
    private String name;
    private String description;
    private String status;        
    private String visibility;    
    private LocalDate startDate;
    
    // [QUAN TRỌNG] Thêm trường này để Frontend lọc
    private LocalDate endDate; 

    private int totalCheckins;
    
    // Cờ báo hiệu có bài đăng mới
    private boolean hasNewUpdates; 

    private List<CheckinResponse> checkins; 
}