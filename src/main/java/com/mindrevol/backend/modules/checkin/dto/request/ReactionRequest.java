package com.mindrevol.backend.modules.checkin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReactionRequest {
    @NotBlank(message = "Emoji là bắt buộc")
    private String emoji; // Nhận chuỗi unicode (ví dụ: "❤️", "🤣", "🚀")

    private String mediaUrl; // Giữ nguyên field này của bạn
}