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
    private LocalDate endDate; 

    // --- CÁC TRƯỜNG CHO CARD UI ---
    private String thumbnailUrl;       
    private String theme;              
    
    // --- [THÊM MỚI] ---
    private String themeColor;
    private String avatar;
    // ------------------

    private List<String> memberAvatars;
    private int totalMembers;          
    private long daysRemaining;        

    private int totalCheckins;         
    private boolean hasNewUpdates;     
    private List<CheckinResponse> checkins; 
}