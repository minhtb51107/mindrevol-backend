package com.mindrevol.backend.modules.report.dto;

import com.mindrevol.backend.modules.report.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResolveReportRequest {
    
    @NotNull(message = "Trạng thái xử lý là bắt buộc (RESOLVED hoặc REJECTED)")
    private ReportStatus status; 

    private String adminNote; // Ghi chú của Admin (VD: "Nội dung vi phạm bản quyền, đã xóa")

    // --- CÁC HÀNH ĐỘNG THỰC THI ---
    private boolean banUser;       // True -> Khóa tài khoản người bị báo cáo
    private boolean deleteContent; // True -> Xóa nội dung (Check-in hoặc Comment) bị báo cáo
}