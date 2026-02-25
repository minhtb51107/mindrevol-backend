package com.mindrevol.backend.modules.auth.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.modules.auth.dto.request.LogoutRequest;
import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.auth.dto.response.UserSessionResponse;
import com.mindrevol.backend.modules.auth.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth: Session", description = "Quản lý phiên đăng nhập và thiết bị")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/refresh-token")
    @Operation(summary = "Làm mới Token", description = "Cấp lại Access Token mới từ Refresh Token.")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        String token = refreshToken.startsWith("Bearer ") ? refreshToken.substring(7) : refreshToken;
        JwtResponse jwtResponse = sessionService.refreshToken(token);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất", description = "Hủy bỏ Refresh Token hiện tại.")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody LogoutRequest request) {
        sessionService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công."));
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Danh sách thiết bị", description = "Xem các nơi đang đăng nhập.")
    public ResponseEntity<ApiResponse<List<UserSessionResponse>>> getSessions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String currentToken) {
        List<UserSessionResponse> sessions = sessionService.getAllSessions(userDetails.getUsername(), currentToken);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Đăng xuất thiết bị", description = "Xóa một phiên đăng nhập cụ thể.")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        sessionService.revokeSession(sessionId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Đã đăng xuất thiết bị thành công."));
    }
}