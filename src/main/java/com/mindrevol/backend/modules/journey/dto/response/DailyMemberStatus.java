package com.mindrevol.backend.modules.journey.dto.response;

public enum DailyMemberStatus {
    COMPLETED,      // Đã check-in thành công
    FAILED,         // Check-in bị từ chối hoặc thất bại
    COMEBACK,       // Hôm qua trễ/fail, nay đã check-in
    LATE_SOON,      // Chưa check-in và sắp hết giờ
    NORMAL,         // Chưa check-in, còn nhiều thời gian
    REST            // Đang nghỉ phép (nếu có tính năng này)
}