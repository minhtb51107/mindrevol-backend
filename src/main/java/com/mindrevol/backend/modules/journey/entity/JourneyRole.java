package com.mindrevol.backend.modules.journey.entity;

public enum JourneyRole {
    OWNER,  // Người tạo hành trình (nếu hành trình độc lập)
    MEMBER, // Thành viên bình thường (nếu hành trình độc lập)
    GUEST   // Khách mời (chỉ tham gia Journey, không vào Box)
}