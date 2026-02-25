package com.mindrevol.backend.modules.box.dto.response;

import com.mindrevol.backend.modules.box.entity.BoxRole;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BoxMemberResponse {
    private String userId;
    private String fullname;
    private String avatarUrl;
    private BoxRole role;
    private LocalDateTime joinedAt;
}