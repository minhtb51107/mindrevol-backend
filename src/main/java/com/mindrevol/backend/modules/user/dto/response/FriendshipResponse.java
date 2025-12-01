package com.mindrevol.backend.modules.user.dto.response;

import com.mindrevol.backend.modules.user.entity.FriendshipStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class FriendshipResponse {
    private Long id;
    private UserSummaryResponse friend; // Thông tin người bạn (đã có class này trong code cũ của bạn)
    private FriendshipStatus status;
    private boolean isRequester; // True nếu mình là người gửi lời mời
    private LocalDateTime createdAt;
}