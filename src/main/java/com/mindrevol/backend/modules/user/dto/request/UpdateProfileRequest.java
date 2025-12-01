package com.mindrevol.backend.modules.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    private String fullname;

    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Handle chỉ được chứa chữ cái, số, dấu chấm và gạch dưới")
    @Size(min = 3, max = 30, message = "Handle phải từ 3 đến 30 ký tự")
    private String handle;

    @Size(max = 150, message = "Tiểu sử không được quá 150 ký tự")
    private String bio;

    @Size(max = 255, message = "Website không hợp lệ")
    private String website;

    private String avatarUrl;
}