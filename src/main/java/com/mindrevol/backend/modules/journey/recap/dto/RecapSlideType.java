package com.mindrevol.backend.modules.journey.recap.dto;

public enum RecapSlideType {
    INTRO,          // Màn hình mở đầu (Tên hành trình, ngày bắt đầu)
    FIRST_CHECKIN,  // Ảnh check-in đầu tiên
    MOST_LIKED,     // Ảnh được nhiều tim nhất
    STREAK_RECORD,  // Khoe chuỗi cao nhất đạt được
    COMEBACK,       // Khoảnh khắc quay trở lại (nếu có)
    STATS,          // Thống kê tổng quan (Tổng số ảnh, tỷ lệ hoàn thành)
    OUTRO           // Màn hình kết thúc
}