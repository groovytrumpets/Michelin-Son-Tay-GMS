package com.g42.platform.gms.billing.infrastructure.repository;

import com.g42.platform.gms.billing.infrastructure.entity.PaymentTransactionJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PaymentTransactionJpaRepo extends JpaRepository<PaymentTransactionJpa,Integer> {

    /** Mọi giao dịch thu tiền của một loạt hoá đơn — dùng khi huỷ hoá đơn của phiếu trùng. */
    List<PaymentTransactionJpa> findByBillIdIn(Collection<Integer> billIds);
}
