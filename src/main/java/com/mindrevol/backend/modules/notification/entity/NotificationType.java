package com.mindrevol.backend.modules.notification.entity;

public enum NotificationType {
    // System & Account
    SYSTEM,
    WELCOME,
    
    // Social Interaction
    FRIEND_REQUEST,
    FRIEND_ACCEPTED,
    
    // Journey & Check-in
    JOURNEY_INVITE,
    JOURNEY_JOINED,
    NEW_POST,           // Ai đó đăng bài trong Journey
    REACTION,           // Ai đó thả tim
    COMMENT,            // Ai đó bình luận
    MENTION,            // Ai đó tag bạn
    
    // Recap
    RECAP_READY         // Video recap đã sẵn sàng
, CHECKIN
}