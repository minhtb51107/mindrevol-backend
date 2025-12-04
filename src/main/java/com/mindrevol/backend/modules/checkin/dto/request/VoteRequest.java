package com.mindrevol.backend.modules.checkin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VoteRequest {
    @NotNull(message = "Quyết định duyệt là bắt buộc")
    private Boolean isApproved; // true = Vote Hợp lệ, false = Vote Từ chối
}