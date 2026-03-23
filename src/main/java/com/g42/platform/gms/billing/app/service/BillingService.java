package com.g42.platform.gms.billing.app.service;

import com.g42.platform.gms.billing.api.dto.ServiceBillDto;
import com.g42.platform.gms.billing.api.mapper.ServiceBillDtoMapper;
import com.g42.platform.gms.billing.domain.entity.ServiceBill;
import com.g42.platform.gms.billing.domain.exception.BillingErrorCode;
import com.g42.platform.gms.billing.domain.exception.BillingException;
import com.g42.platform.gms.billing.domain.repository.BillingRepository;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.repository.EstimateRepository;
import com.g42.platform.gms.promotion.domain.entity.Promotion;
import com.g42.platform.gms.promotion.domain.repository.PromotionRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BillingService {
    @Autowired
    private BillingRepository billingRepository;
    @Autowired
    private ServiceBillDtoMapper serviceBillDtoMapper;
    @Autowired
    private EstimateRepository estimateRepository;
    @Autowired
    private ServiceTicketRepository serviceTicketRepository;
    @Autowired
    private PromotionRepo promotionRepo;
        //todo: get available promotion
    @Transactional
    public ServiceBillDto createNewBilling(ServiceBillDto serviceBillDto) {
        ServiceBill serviceBill = serviceBillDtoMapper.mapToEntity(serviceBillDto);
        //todo: check estimate match serviceTicket
        Estimate estimate = estimateRepository.findEstimateByServiceIdAndLatestVerson(serviceBillDto.getServiceTicketId());
        System.out.println("DEBUG: Estimate: " + estimate.getId());
        ServiceTicketJpa serviceTicket = serviceTicketRepository.findByServiceTicketId(serviceBillDto.getServiceTicketId());
        validateBillingRequest(estimate, serviceTicket);
        serviceBill.setEstimateId(estimate.getId());
        //todo: check promotion available for billing
//        List<Promotion> promotion = resolvePromotion(serviceBillDto);
        //todo: change status of estimate and service ticket
        ServiceBill saved = billingRepository.createNewBilling(serviceBill);
        return serviceBillDtoMapper.mapToDto(saved);
    }

//    private List<Promotion> resolvePromotion(ServiceBillDto serviceBillDto) {
//        List<Promotion> promotions = promotionRepo.getListOfBillingPromotion(serviceBillDto);
//
//    }

    private void validateBillingRequest(Estimate estimate, ServiceTicketJpa serviceTicket) {
        if (serviceTicket == null) {
            throw new BillingException("Service Ticket not found!", BillingErrorCode.SERVICE_TICKET_404);
        }
        if (estimate == null) {
            throw new BillingException("Estimate not found!", BillingErrorCode.ESTIMATE_404);
        }
        if (!estimate.getServiceTicketId().equals(serviceTicket.getServiceTicketId())) {
            throw new BillingException("Service Ticket and Estimate not match", BillingErrorCode.ESTIMATE_NOT_MATCH_SERVICE_TICKET);
        }
    }
}
