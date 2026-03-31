package com.mindrevol.backend.modules.checkin.dto.request;

import lombok.Data;

@Data
public class UpdateCheckinRequest {
    private String caption;
    private String journeyId; // [THÊM MỚI] ID hành trình muốn chuyển vào
}