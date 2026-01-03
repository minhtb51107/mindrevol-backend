package com.mindrevol.backend.modules.journey.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class JourneyAlertResponse {
    // Số lượng lời mời tham gia hành trình (Người khác mời mình)
    private long journeyPendingInvitations; 
    
    // Số lượng yêu cầu xin gia nhập cần duyệt (Mình là chủ phòng)
    private long waitingApprovalRequests;
    
    // Danh sách ID các hành trình có yêu cầu cần duyệt (Để hiện chấm đỏ trên từng Card)
    private List<String> journeyIdsWithRequests; 
    
    // Helper để frontend dễ check có chấm đỏ tổng hay không
    public boolean hasAnyAlert() {
        return journeyPendingInvitations > 0 || waitingApprovalRequests > 0;
    }
}