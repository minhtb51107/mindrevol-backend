package com.mindrevol.backend.modules.checkin.entity;

public enum CheckinStatus {
    NORMAL,     // Check-in bình thường
    FAILED,     // Thất bại (Mất chuỗi nếu không có cơ chế lazy)
    COMEBACK,   // Quay lại sau thất bại
    REST        // Nghỉ phép (Dùng vé Freeze -> Giữ chuỗi) <--- MỚI
}