package com.mindrevol.backend.modules.journey.recap.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class JourneyRecapResponse {
    private String journeyId; // [UUID] String
    private String journeyName;
    private String musicTrackUrl;
    private List<RecapSlide> slides;
}