package com.mindrevol.backend.modules.payment.entity;

import com.mindrevol.backend.common.entity.BaseEntity;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_payment_gateway_ref", columnList = "gateway_ref_id", unique = true) // Quan trọng: Chặn trùng lặp
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "amount")
    private long amount;

    @Column(name = "gateway") // SePay, Casso...
    private String gateway;

    @Column(name = "gateway_ref_id", nullable = false, unique = true) // ID giao dịch từ SePay (để chống trùng)
    private String gatewayRefId;

    @Column(name = "content")
    private String content;

    @Column(name = "status") // SUCCESS, PENDING
    private String status;
}