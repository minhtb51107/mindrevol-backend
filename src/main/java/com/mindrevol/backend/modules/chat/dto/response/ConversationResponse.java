package com.mindrevol.backend.modules.chat.dto.response;

import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ConversationResponse {
    private Long id;
    
    // Thông tin người chat cùng (đã lọc sẵn, không cần frontend check user1/user2)
    private UserSummaryResponse partner; 
    
    // Preview tin nhắn cuối
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private Long lastSenderId;
    
    // Số tin chưa đọc (để hiện badge đỏ)
    private long unreadCount;
    
    // Trạng thái (ACTIVE, BLOCKED...)
    private String status;
}