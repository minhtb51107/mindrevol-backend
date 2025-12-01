package com.mindrevol.backend.modules.user.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserSummaryResponse {
    
    private Long id;
    
    private String handle;
    
    private String fullname;
    
    private String avatarUrl;
    
    private boolean isOnline;
}