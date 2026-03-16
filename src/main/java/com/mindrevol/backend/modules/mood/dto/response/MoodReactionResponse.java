package com.mindrevol.backend.modules.mood.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodReactionResponse {
    private String userId;
    private String fullname;
    private String avatarUrl;
    private String emoji;
}
