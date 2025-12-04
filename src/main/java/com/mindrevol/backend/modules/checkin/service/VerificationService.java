package com.mindrevol.backend.modules.checkin.service;

import com.mindrevol.backend.modules.user.entity.User;
import java.util.UUID;

public interface VerificationService {
    // Vote (Đồng ý hoặc Từ chối)
    void castVote(UUID checkinId, User voter, boolean isApproved);
}