package com.mindrevol.backend.modules.notification.entity;

public enum NotificationType {
    SYSTEM,             // Thông báo hệ thống chung
    FRIEND_REQUEST,     // A gửi lời mời kết bạn
    FRIEND_ACCEPTED,    // A đã chấp nhận kết bạn
    JOURNEY_INVITE,     // A mời vào hành trình
    JOURNEY_JOINED,     // A đã tham gia hành trình
    CHECKIN_REMINDER,   // Nhắc nhở chưa check-in (Sắp hết ngày)
    STREAK_LOST,        // Thông báo mất chuỗi (An ủi)
    STREAK_SAVED,       // Thông báo dùng vé đóng băng thành công
    REACTION,           // A đã thả tim check-in của bạn
    COMMENT,            // A đã bình luận
    COMEBACK            // Bạn bè A đã quay trở lại (Comeback)
}