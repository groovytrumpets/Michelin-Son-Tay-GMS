package com.g42.platform.gms.billing.infrastructure;

import com.g42.platform.gms.billing.domain.entity.PaymentTransaction;
import com.g42.platform.gms.billing.domain.repository.PaymentTransationRepo;

import com.g42.platform.gms.billing.infrastructure.entity.PaymentTransactionJpa;
import com.g42.platform.gms.billing.infrastructure.mapper.PaymentTransactionJpaMapper;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.g42.platform.gms.billing.infrastructure.repository.PaymentTransactionJpaRepo;
@Repository
@AllArgsConstructor
public class PaymentTransactionRepoIml implements PaymentTransationRepo {
    @Autowired
    private PaymentTransactionJpaRepo paymentTransationRepo;
    @Autowired
    private PaymentTransactionJpaMapper paymentTransactionJpaMapper;
    @Override
    public PaymentTransaction createNewPayment(PaymentTransaction paymentTransactionDto) {
        PaymentTransactionJpa  paymentTransactionJpa = paymentTransactionJpaMapper.toJpa(paymentTransactionDto);
        PaymentTransactionJpa saved = paymentTransationRepo.save(paymentTransactionJpa);
        return paymentTransactionJpaMapper.toDomain(saved);
    }
}
