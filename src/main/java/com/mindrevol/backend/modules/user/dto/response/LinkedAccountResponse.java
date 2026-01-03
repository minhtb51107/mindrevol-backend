package com.mindrevol.backend.modules.user.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LinkedAccountResponse {
    private String provider; 
    private String email;    
    private String avatarUrl;
    private boolean connected; 
}