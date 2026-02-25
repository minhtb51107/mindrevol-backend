package com.mindrevol.backend.modules.auth.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.modules.auth.dto.request.*;
import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.auth.service.RegistrationService;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth: Registration", description = "Các API liên quan đến đăng ký và kích hoạt tài khoản")
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/check-email")
    @Operation(summary = "Kiểm tra Email", description = "Kiểm tra xem email đã tồn tại chưa.")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> checkEmail(@Valid @RequestBody CheckEmailRequest request) {
        UserSummaryResponse summary = registrationService.checkEmail(request.getEmail());
        if (summary != null) {
            return ResponseEntity.ok(ApiResponse.success(summary, "Email đã tồn tại"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Email hợp lệ"));
    }

    @PostMapping("/check-handle")
    @Operation(summary = "Kiểm tra Handle", description = "Kiểm tra ID người dùng đã có chưa.")
    public ResponseEntity<ApiResponse<Boolean>> checkHandle(@Valid @RequestBody CheckHandleRequest request) {
        boolean exists = registrationService.isHandleExists(request.getHandle());
        if (exists) {
            return ResponseEntity.ok(ApiResponse.success(true, "Handle đã được sử dụng"));
        }
        return ResponseEntity.ok(ApiResponse.success(false, "Handle hợp lệ"));
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký B1: Gửi OTP", description = "Nhập thông tin và gửi OTP về email.")
    public ResponseEntity<ApiResponse<Void>> registerStep1(@Valid @RequestBody RegisterRequest request) {
        registrationService.registerUserStep1(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Mã xác thực (OTP) đã được gửi đến email của bạn."));
    }

    @PostMapping("/register/verify")
    @Operation(summary = "Đăng ký B2: Xác thực OTP", description = "Xác thực OTP và tạo tài khoản.")
    public ResponseEntity<ApiResponse<JwtResponse>> verifyRegisterOtp(
            @Valid @RequestBody VerifyRegisterOtpRequest request,
            HttpServletRequest servletRequest) {
        JwtResponse jwtResponse = registrationService.verifyRegisterOtp(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse, "Đăng ký thành công!"));
    }

    @PostMapping("/register/resend")
    @Operation(summary = "Gửi lại OTP", description = "Gửi lại mã OTP đăng ký mới.")
    public ResponseEntity<ApiResponse<Void>> resendRegisterOtp(@Valid @RequestBody ResendRegisterOtpRequest request) {
        registrationService.resendRegisterOtp(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Mã xác thực mới đã được gửi."));
    }

    @GetMapping("/activate")
    @Operation(summary = "Kích hoạt tài khoản (Link)", description = "API kích hoạt qua link email.")
    public ResponseEntity<ApiResponse<Void>> activateAccount(@RequestParam("token") String token) {
        registrationService.activateUserAccount(token);
        return ResponseEntity.ok(ApiResponse.success("Kích hoạt tài khoản thành công!"));
    }
}