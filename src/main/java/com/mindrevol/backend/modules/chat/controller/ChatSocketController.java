package com.mindrevol.backend.modules.chat.controller;

import com.mindrevol.backend.modules.chat.dto.request.TypingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Nhận sự kiện Typing từ Client A gửi lên
     * Client gửi tới: /app/chat/typing
     */
    @MessageMapping("/chat/typing")
    public void handleTypingEvent(@Payload TypingEvent event) {
        // Forward ngay lập tức cho Client B (receiverId)
        // Client B sẽ subscribe: /user/queue/typing
        
        messagingTemplate.convertAndSendToUser(
            String.valueOf(event.getReceiverId()),
            "/queue/typing",
            event
        );
    }
}