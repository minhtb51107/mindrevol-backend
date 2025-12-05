package com.mindrevol.backend.modules.payment.repository;

import com.mindrevol.backend.modules.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    // Tìm xem mã giao dịch này đã tồn tại chưa
    boolean existsByGatewayRefId(String gatewayRefId);
}