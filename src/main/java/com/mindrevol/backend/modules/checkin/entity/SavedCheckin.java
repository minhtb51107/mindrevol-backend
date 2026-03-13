package com.mindrevol.backend.modules.checkin.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "saved_checkins",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "checkin_id"})}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedCheckin extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_id", nullable = false)
    private Checkin checkin;
}