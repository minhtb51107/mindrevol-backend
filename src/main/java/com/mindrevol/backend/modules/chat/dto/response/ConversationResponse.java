package com.mindrevol.backend.modules.chat.dto.response;

import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ConversationResponse {
    private String id; 
    
    private UserSummaryResponse partner; 
    
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private String lastSenderId; 
    
    private long unreadCount;
    private String status;
    private String boxId;

    // [THÊM MỚI] Gửi kèm Tên và Avatar của Box xuống cho UI hiển thị
    private String boxName;
    private String boxAvatar;
}