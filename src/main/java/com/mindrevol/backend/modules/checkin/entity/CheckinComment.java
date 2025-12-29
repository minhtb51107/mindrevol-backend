package com.mindrevol.backend.modules.checkin.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "checkin_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // [FIX] Dùng SuperBuilder
public class CheckinComment extends BaseEntity {

    // [ĐÃ XÓA] @Id UUID id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_id", nullable = false)
    private Checkin checkin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    // BaseEntity đã lo createdAt
}