package com.mindrevol.backend.modules.journey.dto.response;

public enum WidgetStatus {
    PENDING,            // Chưa làm gì hôm nay (Đang chờ)
    COMPLETED,          // Đã hoàn thành (Check-in Normal)
    REST,               // Đã dùng vé nghỉ phép
    COMEBACK_COMPLETED, // Vừa mới quay trở lại thành công hôm nay
    FAILED_STREAK       // Đã mất chuỗi (Hôm qua không làm, hôm nay chưa làm)
}