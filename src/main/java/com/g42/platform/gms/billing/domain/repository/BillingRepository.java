package com.g42.platform.gms.billing.domain.repository;

import com.g42.platform.gms.billing.domain.entity.ServiceBill;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingRepository {
    ServiceBill createNewBilling(ServiceBill serviceBill);
}
