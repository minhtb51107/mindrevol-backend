package com.mindrevol.backend.modules.user.dto.response;

import com.mindrevol.backend.modules.user.entity.FriendshipStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class FriendshipResponse {
    private String id; // [UUID] String (ID của Friendship)
    private UserSummaryResponse friend; 
    private FriendshipStatus status;
    private boolean isRequester; 
    private LocalDateTime createdAt;
}