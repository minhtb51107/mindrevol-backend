package com.mindrevol.backend.modules.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTask implements Serializable {
    private String toEmail;
    private String subject;
    private String content;
    private int retryCount; 
}