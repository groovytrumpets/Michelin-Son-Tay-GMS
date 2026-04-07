package com.g42.platform.gms.billing.infrastructure;

import com.g42.platform.gms.billing.domain.entity.ServiceBill;
import com.g42.platform.gms.billing.domain.repository.BillingRepository;
import com.g42.platform.gms.billing.infrastructure.entity.ServiceBillJpa;
import com.g42.platform.gms.billing.infrastructure.mapper.ServiceBillJpaMapper;
import com.g42.platform.gms.billing.infrastructure.repository.ServiceBillJpaRepo;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class BillingRepoImpl implements BillingRepository {
    @Autowired
    private ServiceBillJpaRepo serviceBillJpaRepo;
    @Autowired
    private ServiceBillJpaMapper serviceBillJpaMapper;

    @Override
    public ServiceBill createNewBilling(ServiceBill serviceBill) {
        ServiceBillJpa serviceBillJpa = serviceBillJpaMapper.toEntity(serviceBill);
        ServiceBillJpa saved= serviceBillJpaRepo.save(serviceBillJpa);
        return serviceBillJpaMapper.toDomain(saved);
    }

    @Override
    public ServiceBill getBillingByBillingId(Integer billId) {
        ServiceBillJpa serviceBillJpa = serviceBillJpaRepo.findById(billId).orElse(null);
        return serviceBillJpaMapper.toDomain(serviceBillJpa);
    }

    @Override
    public void save(ServiceBill serviceBill) {
        ServiceBillJpa saved = serviceBillJpaRepo.save(serviceBillJpaMapper.toEntity(serviceBill));
    }
}
