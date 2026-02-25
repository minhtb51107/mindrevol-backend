package com.mindrevol.backend.modules.box.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boxes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("deleted_at IS NULL") // Tự động filter các record đã xóa mềm
public class Box extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    // --- TRANG TRÍ ---
    @Column(name = "avatar")
    private String avatar; // Chứa link ảnh hoặc 1 ký tự Emoji

    @Column(name = "cover_image")
    private String coverImage; // Ảnh bìa trên cùng

    @Column(name = "theme_color", length = 20)
    private String themeColor; // Mã màu HEX (vd: #3B82F6)
    // -----------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "is_archived", nullable = false)
    @Builder.Default
    private Boolean isArchived = false;

    @OneToMany(mappedBy = "box", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BoxMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "box")
    @Builder.Default
    private List<com.mindrevol.backend.modules.journey.entity.Journey> journeys = new ArrayList<>();
}