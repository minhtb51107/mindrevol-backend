package com.mindrevol.backend.modules.checkin.service;

import com.mindrevol.backend.modules.user.entity.User;
import java.util.UUID; // Xóa import thừa này đi

public interface VerificationService {
    // [FIX] UUID -> Long
    void castVote(Long checkinId, User voter, boolean isApproved);
}