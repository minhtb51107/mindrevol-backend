package com.mindrevol.backend.modules.system.dto;

import lombok.Data;

@Data
public class CreateFeedbackRequest {
    private String type; // BUG, FEATURE_REQUEST...
    private String content;
    private String appVersion;
    private String deviceName;
    private String screenshotUrl;
}