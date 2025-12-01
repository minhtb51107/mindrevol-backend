package com.mindrevol.backend.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisUserSession implements Serializable {
    private String id;          
    private String email;       
    private String refreshToken;
    private String ipAddress;   
    private String userAgent;  
    private long expiredAt;     
}