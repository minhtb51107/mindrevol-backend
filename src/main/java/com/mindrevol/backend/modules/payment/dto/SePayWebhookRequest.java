package com.mindrevol.backend.modules.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SePayWebhookRequest {
    private long id; 
    
    @JsonProperty("gateway")
    private String gateway; 
    
    @JsonProperty("transactionDate")
    private String transactionDate;
    
    @JsonProperty("accountNumber")
    private String accountNumber;
    
    @JsonProperty("subAccount")
    private String subAccount;
    
    @JsonProperty("transferAmount")
    private long transferAmount; 
    
    @JsonProperty("transferType")
    private String transferType; 
    
    @JsonProperty("content")
    private String content; 
    
    @JsonProperty("code")
    private String code;
}