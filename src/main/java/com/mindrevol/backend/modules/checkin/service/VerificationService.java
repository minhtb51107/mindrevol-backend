package com.mindrevol.backend.modules.checkin.service;

import com.mindrevol.backend.modules.user.entity.User;

public interface VerificationService {
    // [UUID] Long -> String
    void castVote(String checkinId, User voter, boolean isApproved);
}