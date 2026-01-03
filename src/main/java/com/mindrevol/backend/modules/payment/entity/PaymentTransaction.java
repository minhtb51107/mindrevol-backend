package com.mindrevol.backend.modules.payment.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_payment_gateway_ref", columnList = "gateway_ref_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PaymentTransaction extends BaseEntity {

    // [UUID] ID String từ BaseEntity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // User ID là String

    @Column(name = "amount")
    private long amount;

    @Column(name = "gateway") // SePay, Casso...
    private String gateway;

    @Column(name = "gateway_ref_id", nullable = false, unique = true)
    private String gatewayRefId;

    @Column(name = "content")
    private String content;

    @Column(name = "status") 
    private String status;
}