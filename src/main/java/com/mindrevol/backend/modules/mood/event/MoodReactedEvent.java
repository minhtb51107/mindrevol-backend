package com.mindrevol.backend.modules.mood.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodReactedEvent {
    private String moodId;
    private String boxId;
    private String reactorId;    // Người vừa bấm thả tim
    private String moodOwnerId;  // Chủ nhân của bong bóng (Người sẽ nhận được thông báo)
    private String emoji;        // Thả biểu tượng gì
}