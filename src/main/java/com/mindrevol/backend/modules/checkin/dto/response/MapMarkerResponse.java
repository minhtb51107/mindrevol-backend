package com.mindrevol.backend.modules.checkin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapMarkerResponse {
    private String checkinId;
    private Double latitude;
    private Double longitude;
    private String thumbnailUrl;
    private String userAvatar;
    private String fullname;
}