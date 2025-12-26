package com.mindrevol.backend.modules.journey.entity;

public enum JourneyStatus {
    DRAFT,
    ACTIVE,      // Đang diễn ra (Hiện trang chủ)
    PAUSED,
    COMPLETED,   // Đã kết thúc (Hiện phần Recap)
    ARCHIVED,
    DELETED
}