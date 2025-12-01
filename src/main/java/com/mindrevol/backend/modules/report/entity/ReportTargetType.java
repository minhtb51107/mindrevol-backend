package com.mindrevol.backend.modules.report.entity;

public enum ReportTargetType {
    CHECKIN,    // Báo cáo một bài check-in (ảnh xấu)
    USER,       // Báo cáo một người dùng (mạo danh, quấy rối)
    COMMENT,    // Báo cáo bình luận (chửi bới)
    JOURNEY     // Báo cáo một hành trình (nhóm lừa đảo...)
}