package com.mindrevol.backend.modules.checkin.entity;

public enum CheckinStatus {
    NORMAL,                 // Check-in bình thường (Đã duyệt hoặc không cần duyệt)
    FAILED,                 // Thất bại
    COMEBACK,               // Quay lại sau thất bại
    REST,                   // Nghỉ phép
    PENDING_VERIFICATION,   // MỚI: Đang chờ cộng đồng vote
    REJECTED                // MỚI: Bị cộng đồng từ chối (Ảnh fake)
, PENDING
}