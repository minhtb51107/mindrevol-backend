package com.mindrevol.backend.modules.box.event;

import com.mindrevol.backend.modules.box.entity.Box;
import com.mindrevol.backend.modules.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BoxMemberAddedEvent {
    private final Box box;
    private final User adder;      // Người mời
    private final User newMember;  // Người được mời
}