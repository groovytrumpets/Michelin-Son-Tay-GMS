package com.g42.platform.gms.billing.infrastructure.repository;

import com.g42.platform.gms.billing.infrastructure.entity.PaymentTransactionJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionJpaRepo extends JpaRepository<PaymentTransactionJpa,Integer> {
}
