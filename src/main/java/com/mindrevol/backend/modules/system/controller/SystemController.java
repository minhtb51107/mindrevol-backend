package com.mindrevol.backend.modules.system.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.modules.system.dto.CreateFeedbackRequest;
import com.mindrevol.backend.modules.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/configs")
    @Operation(summary = "Lấy các link MXH, Policy, Terms, Email hỗ trợ")
    public ResponseEntity<ApiResponse<Map<String, String>>> getConfigs() {
        // Trả về dạng Map: {"SOCIAL_TIKTOK": "https://...", "TERMS_URL": "https://..."}
        // Frontend sẽ dùng cái này để render mục "Giới thiệu"
        return ResponseEntity.ok(ApiResponse.success(systemService.getPublicConfigs()));
    }

    @PostMapping("/feedback")
    @Operation(summary = "Gửi báo cáo lỗi hoặc đề xuất tính năng")
    public ResponseEntity<ApiResponse<Void>> sendFeedback(@RequestBody CreateFeedbackRequest request) {
        systemService.submitFeedback(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Cảm ơn bạn đã đóng góp ý kiến!"));
    }
}