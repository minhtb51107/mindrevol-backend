package com.mindrevol.backend.modules.journey.entity;

public enum JourneyVisibility {
    PUBLIC,     // Công khai: Ai cũng có thể tìm thấy và xem (VD: Lớp học mở, Challenge cộng đồng)
    PRIVATE     // Riêng tư: Chỉ thành viên được mời hoặc có link mới thấy (VD: Nhóm bạn, Couple)
, FRIEND_ONLY
}