// File: java/com/mindrevol/backend/modules/journey/dto/response/UserActiveJourneyResponse.java

package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class UserActiveJourneyResponse {
    private UUID id;
    private String name;
    private String description;
    private String status;        // IN_PROGRESS
    private String visibility;    
    private LocalDate startDate;
    private int totalCheckins;    // Tổng số bài
    
    // [CẬP NHẬT QUAN TRỌNG]: Trả về danh sách toàn bộ bài đăng trong hành trình này
    private List<CheckinResponse> checkins; 
}