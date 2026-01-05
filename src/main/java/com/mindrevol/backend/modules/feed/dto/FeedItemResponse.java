package com.mindrevol.backend.modules.feed.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;

// Cấu hình Jackson để khi trả về JSON, nó tự thêm field "type"
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CheckinResponse.class, name = "POST"),
        @JsonSubTypes.Type(value = AdFeedItemResponse.class, name = "INTERNAL_AD"),
        @JsonSubTypes.Type(value = AdFeedItemResponse.class, name = "AFFILIATE_AD"),
        @JsonSubTypes.Type(value = AdFeedItemResponse.class, name = "GOOGLE_AD")
})
public interface FeedItemResponse {
    FeedItemType getType();
}