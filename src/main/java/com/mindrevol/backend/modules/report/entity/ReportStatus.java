package com.mindrevol.backend.modules.report.entity;

public enum ReportStatus {
    PENDING,    // Đang chờ admin xem
    RESOLVED,   // Đã xử lý (đã xóa bài/ban user)
    REJECTED    // Báo cáo sai, không vi phạm
}