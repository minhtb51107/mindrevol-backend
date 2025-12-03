package com.mindrevol.backend.modules.journey.recap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoTask implements Serializable {
    private Long userId;
    private UUID journeyId;
    private List<String> imageUrls; // Danh sách ảnh cần ghép thành video
    private String musicUrl;        // Link nhạc nền (nếu có)
    private int retryCount;
}