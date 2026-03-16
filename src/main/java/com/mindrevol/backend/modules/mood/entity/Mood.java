package com.mindrevol.backend.modules.mood.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.box.entity.Box;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "moods", indexes = {
        @Index(name = "idx_mood_box", columnList = "box_id"),
        @Index(name = "idx_mood_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Mood extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_id", nullable = false)
    private Box box;

    @Column(nullable = false, length = 50)
    private String icon; // Chứa Emoji, ví dụ: "🔥", "😭"

    @Column(length = 100)
    private String message; // Tùy chọn: Ghi chú ngắn gọn (nếu UI có)

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // Thời gian bốc hơi (sau 24h)

    @OneToMany(mappedBy = "mood", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MoodReaction> reactions = new ArrayList<>();
}