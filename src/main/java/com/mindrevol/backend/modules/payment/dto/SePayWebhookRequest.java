package com.mindrevol.backend.modules.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SePayWebhookRequest {
    private long id; // ID giao dịch tại SePay
    
    @JsonProperty("gateway")
    private String gateway; // Ngân hàng (MB, VCB...)
    
    @JsonProperty("transactionDate")
    private String transactionDate;
    
    @JsonProperty("accountNumber")
    private String accountNumber;
    
    @JsonProperty("subAccount")
    private String subAccount;
    
    @JsonProperty("transferAmount")
    private long transferAmount; // Số tiền vào (quan trọng)
    
    @JsonProperty("transferType")
    private String transferType; // "in" hoặc "out"
    
    @JsonProperty("content")
    private String content; // Nội dung CK (Ví dụ: MINDREVOL 105)
    
    @JsonProperty("code")
    private String code;
}