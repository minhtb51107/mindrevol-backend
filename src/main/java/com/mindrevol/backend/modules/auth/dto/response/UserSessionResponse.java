package com.mindrevol.backend.modules.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@Builder
public class UserSessionResponse {
    private String id;          
    private String ipAddress;
    private String userAgent;   
    private boolean isCurrent;  
    private LocalDateTime expiresAt; 

    public static LocalDateTime mapToLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
}