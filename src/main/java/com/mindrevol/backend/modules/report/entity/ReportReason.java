package com.mindrevol.backend.modules.report.entity;

public enum ReportReason {
    SPAM,                   // Spam, quảng cáo
    NUDITY_OR_SEXUAL,       // Khỏa thân, tình dục
    HATE_SPEECH,            // Ngôn từ thù ghét
    VIOLENCE,               // Bạo lực
    HARASSMENT,             // Quấy rối
    FALSE_INFORMATION,      // Thông tin sai lệch
    COPYRIGHT_INFRINGEMENT, // [MỚI] Vi phạm bản quyền (Bắt buộc theo luật DMCA/Store)
    OTHER                   // Khác
}