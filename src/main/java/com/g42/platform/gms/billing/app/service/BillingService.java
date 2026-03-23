package com.g42.platform.gms.billing.app.service;

import com.g42.platform.gms.billing.api.dto.ServiceBillDto;
import com.g42.platform.gms.billing.api.mapper.ServiceBillDtoMapper;
import com.g42.platform.gms.billing.domain.entity.ServiceBill;
import com.g42.platform.gms.billing.domain.repository.BillingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillingService {
    @Autowired
    private BillingRepository billingRepository;
    @Autowired
    private ServiceBillDtoMapper serviceBillDtoMapper;

    public ServiceBillDto createNewBilling(ServiceBillDto serviceBillDto) {
        ServiceBill serviceBill = serviceBillDtoMapper.mapToEntity(serviceBillDto);
        ServiceBill saved = billingRepository.createNewBilling(serviceBill);
        return serviceBillDtoMapper.mapToDto(saved);
    }
}
