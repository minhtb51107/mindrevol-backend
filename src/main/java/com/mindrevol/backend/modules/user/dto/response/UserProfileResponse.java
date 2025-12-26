package com.mindrevol.backend.modules.user.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
public class UserProfileResponse {
    
    private Long id;
    
    private String email;
    
    private String handle;
    
    private String fullname;
    
    private String avatarUrl;
    
    private String bio;
    
    private String website;
    
    private OffsetDateTime joinedAt;
    
    private String status;
    
    private Set<String> roles;
    
    private long followerCount;
    
    private long followingCount;
    
    private boolean isFollowedByCurrentUser;
    
    private long friendCount; // [THÊM MỚI]
}