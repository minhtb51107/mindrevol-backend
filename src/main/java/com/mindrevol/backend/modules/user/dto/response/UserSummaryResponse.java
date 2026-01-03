package com.mindrevol.backend.modules.user.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserSummaryResponse {
    private String id; // [UUID] String
    private String handle;
    private String fullname;
    private String avatarUrl;
    private boolean isOnline;
    private LocalDateTime lastActiveAt;
    private String friendshipStatus; 
}