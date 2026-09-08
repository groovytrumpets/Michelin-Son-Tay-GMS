package com.g42.platform.gms.billing.infrastructure.repository;

import com.g42.platform.gms.billing.infrastructure.entity.PaymentProofJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentProofJpaRepo extends JpaRepository<PaymentProofJpa, Integer> {

    List<PaymentProofJpa> findByServiceTicketIdOrderByCreatedAtAscPaymentProofIdAsc(Integer serviceTicketId);

    long countByServiceTicketId(Integer serviceTicketId);

    Optional<PaymentProofJpa> findByPaymentProofIdAndServiceTicketId(Integer paymentProofId, Integer serviceTicketId);
}
