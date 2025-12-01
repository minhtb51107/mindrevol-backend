package com.mindrevol.backend.modules.report.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.report.dto.CreateReportRequest;
import com.mindrevol.backend.modules.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createReport(@Valid @RequestBody CreateReportRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        reportService.createReport(currentUserId, request);
        
        // Trả về thành công luôn để User cảm thấy report đã được ghi nhận
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}