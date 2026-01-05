package com.mindrevol.backend.modules.advertising.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.feed.dto.FeedItemType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_ads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemAd extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "cta_link", nullable = false)
    private String ctaLink;

    @Column(name = "cta_text", nullable = false)
    private String ctaText;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "priority")
    @Builder.Default
    private int priority = 1;

    // [MỚI] Loại quảng cáo
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    @Builder.Default
    private FeedItemType type = FeedItemType.INTERNAL_AD;

    // [MỚI] Tags mục tiêu (Lưu dạng chuỗi "coffee,morning,chill")
    @Column(name = "target_tags")
    private String targetTags;
}