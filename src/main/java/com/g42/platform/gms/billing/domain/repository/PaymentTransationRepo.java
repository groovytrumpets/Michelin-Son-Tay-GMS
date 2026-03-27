package com.g42.platform.gms.billing.domain.repository;

import com.g42.platform.gms.billing.domain.entity.PaymentTransaction;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransationRepo {
    PaymentTransaction createNewPayment(PaymentTransaction paymentTransactionDto);
}
