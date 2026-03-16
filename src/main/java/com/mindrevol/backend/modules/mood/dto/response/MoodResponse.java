package com.mindrevol.backend.modules.mood.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodResponse {
    private String id;
    private String boxId;
    private String userId;
    private String fullname;
    private String avatarUrl;

    private String icon; // Ví dụ: "ic_mood_happy"
    private String message;

    private LocalDateTime updatedAt; // Để hiển thị thời gian đăng (ví dụ: 5 phút trước)
    private LocalDateTime expiresAt; // Để Frontend đếm ngược hoặc tự ẩn

    private List<MoodReactionResponse> reactions;
}