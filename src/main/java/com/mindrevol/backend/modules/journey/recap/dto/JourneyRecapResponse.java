package com.mindrevol.backend.modules.journey.recap.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class JourneyRecapResponse {
    private Long journeyId;
    private String journeyName;
    private String musicTrackUrl; // (Optional) Gợi ý bài nhạc nền
    private List<RecapSlide> slides; // Danh sách các slide sẽ chạy
}