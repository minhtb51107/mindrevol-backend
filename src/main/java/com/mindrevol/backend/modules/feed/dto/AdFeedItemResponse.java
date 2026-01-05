package com.mindrevol.backend.modules.feed.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdFeedItemResponse implements FeedItemResponse {
    
    private final FeedItemType type; // Loại quảng cáo (INTERNAL_AD, AFFILIATE_AD...)
    
    private String id;
    private String title;
    private String imageUrl;
    private String ctaLink;
    private String ctaText;
    
    // Nhà cung cấp: "MINDREVOL" (Internal/Affiliate) hoặc "GOOGLE"
    private String adProvider; 
    
    private String adUnitId; // Dùng cho Google AdSense

    @Override
    public FeedItemType getType() {
        return type;
    }
}