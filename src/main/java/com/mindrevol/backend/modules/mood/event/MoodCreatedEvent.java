package com.mindrevol.backend.modules.mood.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodCreatedEvent {
    private String moodId;
    private String boxId;
    private String userId; // Người vừa đăng trạng thái
    private String icon;   // Biểu tượng gì (để lỡ có muốn in ra thông báo: "A đang cảm thấy ic_mood_sad")
}