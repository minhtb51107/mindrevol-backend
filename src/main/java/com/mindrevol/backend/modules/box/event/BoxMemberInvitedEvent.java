package com.mindrevol.backend.modules.box.event;

import com.mindrevol.backend.modules.box.entity.Box;
import com.mindrevol.backend.modules.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BoxMemberInvitedEvent {
    private final Box box;
    private final User inviter;
    private final User invitee;
}