package com.mindrevol.backend.modules.report.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.report.dto.CreateReportRequest;
import com.mindrevol.backend.modules.report.dto.ResolveReportRequest; // Import DTO mới
import com.mindrevol.backend.modules.report.entity.Report;
import com.mindrevol.backend.modules.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Quan trọng
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // API cho User thường: Gửi báo cáo
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createReport(@Valid @RequestBody CreateReportRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        reportService.createReport(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Báo cáo đã được gửi và đang chờ xem xét."));
    }

    // --- ADMIN APIS (BẮT BUỘC PHẢI CÓ ROLE ADMIN) ---

    // 1. Xem danh sách báo cáo chờ xử lý
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<Report>>> getPendingReports(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getPendingReports(pageable)));
    }

    // 2. Xử lý báo cáo (Phạt/Bỏ qua)
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resolveReport(
            @PathVariable Long id,
            @Valid @RequestBody ResolveReportRequest request) {
        
        Long adminId = SecurityUtils.getCurrentUserId();
        reportService.resolveReport(id, adminId, request);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xử lý báo cáo thành công."));
    }
}