package com.mindrevol.backend.modules.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullname;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    private String password;

    @NotBlank(message = "Handle (Username) không được để trống")
    @Size(min = 3, max = 30, message = "Handle phải từ 3 đến 30 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Handle chỉ chứa chữ cái, số, dấu chấm và gạch dưới")
    private String handle;

    // --- MỚI: Thêm ngày sinh để kiểm tra độ tuổi (Luật COPPA/GDPR) ---
    @NotNull(message = "Ngày sinh là bắt buộc")
    @Past(message = "Ngày sinh phải trong quá khứ")
    private LocalDate dateOfBirth;

    // --- MỚI: Bắt buộc đồng ý điều khoản ---
    @AssertTrue(message = "Bạn phải đồng ý với Điều khoản sử dụng và Chính sách quyền riêng tư")
    private boolean agreedToTerms;
}