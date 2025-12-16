package com.mindrevol.backend.modules.chat.dto.request;

import com.mindrevol.backend.modules.chat.entity.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class SendMessageRequest {
    
    @NotNull(message = "Người nhận không được để trống")
    private Long receiverId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String content;

    private MessageType type;

    // [THÊM MỚI]
    private Map<String, Object> metadata;
    
    // [THÊM MỚI] ID tạm từ client để xử lý Optimistic UI
    private String clientSideId;
}