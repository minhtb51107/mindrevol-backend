package com.mindrevol.backend.modules.chat.entity;

public enum MessageDeliveryStatus {
    SENT,       // Đã gửi lên server
    DELIVERED,  // Đã đến máy người nhận (Socket push thành công)
    SEEN        // Người nhận đã mở xem
}