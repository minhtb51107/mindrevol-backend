package com.mindrevol.backend.modules.user.dto.response;

import com.mindrevol.backend.modules.user.entity.FriendshipStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
public class UserProfileResponse {
    
    private String id; // [UUID] String
    private String email;
    private String handle;
    private String fullname;
    private String avatarUrl;
    private String bio;
    private String website;
    private OffsetDateTime joinedAt;
    private String status;
    private Set<String> roles;
    private long followerCount; // Có thể ẩn nếu chưa dùng
    private long followingCount; // Có thể ẩn nếu chưa dùng
    private boolean isFollowedByCurrentUser; // Logic follow (nếu có)
    private long friendCount; 

    // --- CÁC TRƯỜNG MỚI CHO TÍNH NĂNG XEM PROFILE ---
    private FriendshipStatus friendshipStatus; // NONE, PENDING, ACCEPTED, DECLINED
    private boolean isBlockedByMe;   // Mình có chặn họ không
    private boolean isBlockedByThem; // Họ có chặn mình không (để xử lý UI ẩn nội dung)
    private boolean isMe;            // Đây có phải là mình không
}