package com.mindrevol.backend.modules.feed.dto;

public enum FeedItemType {
    POST,           // Bài đăng Check-in thường
    INTERNAL_AD,    // Quảng cáo nội bộ (Bán Gold, Thông báo hệ thống)
    AFFILIATE_AD,   // Quảng cáo Tiếp thị liên kết (Shopee/Lazada - Context Aware)
    GOOGLE_AD       // Quảng cáo AdSense (Dự phòng)
}