package com.mindrevol.backend.modules.payment.repository;

import com.mindrevol.backend.modules.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// [UUID] JpaRepository<PaymentTransaction, String>
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {
    boolean existsByGatewayRefId(String gatewayRefId);
}