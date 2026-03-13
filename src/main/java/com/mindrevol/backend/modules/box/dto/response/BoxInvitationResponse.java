package com.mindrevol.backend.modules.box.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BoxInvitationResponse {
    private Long id;
    private String boxId;
    private String boxName;
    private String boxAvatar;
    private String inviterId;
    private String inviterName;
    private String inviterAvatar;
    private String status;
    private LocalDateTime sentAt;
}