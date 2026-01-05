package com.mindrevol.backend.modules.checkin.dto.response;

import com.mindrevol.backend.modules.checkin.entity.ActivityType;
import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.checkin.entity.CheckinVisibility;
import com.mindrevol.backend.modules.checkin.entity.MediaType; // Import Enum
import com.mindrevol.backend.modules.feed.dto.FeedItemResponse;
import com.mindrevol.backend.modules.feed.dto.FeedItemType;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinResponse implements FeedItemResponse {

    private String id;
    private UserSummaryResponse user;
    private String journeyId;
    private String journeyName;

    // --- MEDIA ---
    private String imageUrl;      // Nếu là Video thì đây là Thumbnail
    private String videoUrl;      // [MỚI] Link video (nếu có)
    private MediaType mediaType;  // [MỚI] IMAGE hoặc VIDEO

    // --- CONTENT ---
    private String caption;
    private String emotion;
    private ActivityType activityType;
    private String activityName;
    private String locationName;
    private List<String> tags;

    // --- STATUS ---
    private CheckinStatus status;
    private CheckinVisibility visibility;
    private LocalDate checkinDate;
    private LocalDateTime createdAt;

    // --- INTERACTION ---
    private int reactionCount;
    private int commentCount;
    private List<CheckinReactionDetailResponse> latestReactions; // 3 người thả tim gần nhất
    
    @Override
    public FeedItemType getType() {
        return FeedItemType.POST;
    }
}