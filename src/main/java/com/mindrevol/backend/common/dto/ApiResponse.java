package com.mindrevol.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int status;
    private String message;
    
    // --- THÊM MỚI ---
    private String errorCode; // Mã lỗi máy đọc (VD: USER_NOT_FOUND)
    // ----------------
    
    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message("Success")
                .data(data)
                .build();
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message, String errorCode) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
    
    // Giữ lại method cũ để tương thích ngược, mặc định errorCode là GENERAL_ERROR
    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .errorCode("GENERAL_ERROR")
                .build();
    }
    
    // Method hỗ trợ trả về data kèm lỗi (ví dụ: validation errors)
    public static <T> ApiResponse<T> error(int status, String message, String errorCode, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .errorCode(errorCode)
                .data(data)
                .build();
    }
}