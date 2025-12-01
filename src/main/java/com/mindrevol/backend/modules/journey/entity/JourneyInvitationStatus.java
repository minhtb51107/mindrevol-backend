package com.mindrevol.backend.modules.journey.entity;

public enum JourneyInvitationStatus {
    PENDING,    // Đang chờ phản hồi
    ACCEPTED,   // Đã chấp nhận
    REJECTED,   // Đã từ chối
    EXPIRED     // Đã hết hạn (nếu cần logic hết hạn sau này)
}