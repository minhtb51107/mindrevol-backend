package com.mindrevol.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Bỏ qua các trường null để JSON gọn nhẹ
public class ApiResponse<T> {

    private int status;       // HTTP Status Code (200, 400, 500...)
    private String message;   // Message cho End-user (VD: "Thành công")
    private String errorCode; // Mã lỗi cho Dev/System (VD: USER_NOT_FOUND)
    
    private String traceId; 

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp = LocalDateTime.now();

    private T data; 

    // 1. Thành công - Có dữ liệu
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message("Success")
                .data(data)
                .build();
    }
    
    // 2. Thành công - Có dữ liệu + Message tùy chỉnh
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .build();
    }

    // 3. Thành công - Chỉ có Message (VD: Xóa, Update status)
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .build();
    }

    // 4. Lỗi - Cơ bản (Tự động lấy TraceID từ MDC)
    public static <T> ApiResponse<T> error(int status, String message, String errorCode) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .errorCode(errorCode)
                .traceId(MDC.get("requestId")) // [QUAN TRỌNG] Tự động lấy ID từ Filter
                .build();
    }
    
    // 5. Lỗi - Kèm dữ liệu (VD: Validation Errors trả về Map lỗi chi tiết)
    public static <T> ApiResponse<T> error(int status, String message, String errorCode, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .errorCode(errorCode)
                .data(data)
                .traceId(MDC.get("requestId"))
                .build();
    }

    // 6. Lỗi - Tương thích ngược (Mặc định GENERAL_ERROR)
    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .errorCode("GENERAL_ERROR")
                .traceId(MDC.get("requestId"))
                .build();
    }
}