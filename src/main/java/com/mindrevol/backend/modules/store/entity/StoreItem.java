package com.mindrevol.backend.modules.store.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "store_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreItem extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code; // Mã sản phẩm (Vd: FREEZE_1)

    @Column(nullable = false)
    private String name; // Tên hiển thị (Vd: "1 Vé Đóng Băng")

    private String description;

    @Column(nullable = false)
    private Long price; // Giá bán (bằng Point)

    @Column(nullable = false)
    private String iconUrl; // Ảnh minh họa sản phẩm

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemEffectType effectType; // Item này làm gì?

    @Column(nullable = false)
    private Integer effectValue; // Giá trị tác dụng (Vd: cộng 1)
    
    @Builder.Default
    private boolean isActive = true;
}