package com.mindrevol.backend.modules.journey.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "journey_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // Dùng SuperBuilder để kế thừa builder từ BaseEntity
public class JourneyRequest extends BaseEntity {

    // [QUAN TRỌNG] Đã xóa trường @Id private UUID id; 
    // Vì nó sẽ dùng ID Long từ BaseEntity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id", nullable = false)
    private Journey journey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;
    
    // Các trường createdAt, updatedAt đã có sẵn trong BaseEntity nên không cần khai báo lại
}