package com.mindrevol.backend.modules.auth.dto;

import com.mindrevol.backend.modules.user.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterTempData implements Serializable {
    // Dữ liệu người dùng nhập
    private String fullname;
    private String email;
    private String password; // Đã mã hóa hoặc chưa (tùy logic service, ta sẽ lưu chưa mã hóa hoặc mã hóa luôn)
    private String handle;
    private LocalDate dateOfBirth;
    private Gender gender;
    
    // Dữ liệu hệ thống sinh ra
    private String otpCode;
    private int retryCount; // Đếm số lần nhập sai
}