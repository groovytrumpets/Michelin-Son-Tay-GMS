package com.g42.platform.gms.billing.app.service;

import com.g42.platform.gms.billing.api.dto.PaymentTransactionDto;
import com.g42.platform.gms.billing.api.dto.ServiceBillDto;
import com.g42.platform.gms.billing.api.mapper.ServiceBillDtoMapper;
import com.g42.platform.gms.billing.domain.entity.PaymentTransaction;
import com.g42.platform.gms.billing.domain.entity.ServiceBill;
import com.g42.platform.gms.billing.domain.enums.BillingStatus;
import com.g42.platform.gms.billing.domain.enums.PaymentStatus;
import com.g42.platform.gms.billing.domain.enums.PaymentTransactionStatus;
import com.g42.platform.gms.billing.domain.exception.BillingErrorCode;
import com.g42.platform.gms.billing.domain.exception.BillingException;
import com.g42.platform.gms.billing.domain.repository.BillingRepository;
import com.g42.platform.gms.billing.domain.repository.PaymentTransationRepo;
import com.g42.platform.gms.common.enums.EstimateEnum;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.repository.EstimateRepository;
import com.g42.platform.gms.promotion.domain.entity.Promotion;
import com.g42.platform.gms.promotion.domain.repository.PromotionRepo;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
    @Autowired
    private PaymentTransationRepo paymentTransationRepo;
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
        Promotion promotion = resolvePromotion(serviceBillDto);
        if (promotion != null) {
            BigDecimal discountAmount = serviceBillDto.getSubTotal()
                    .multiply(promotion.getDiscountPercent())
                    .divide(BigDecimal.valueOf(100));
            serviceBill.setDiscountAmount(discountAmount);
            serviceBill.setFinalAmount(estimate.getTotalPrice().subtract(discountAmount));
        }else {
            serviceBill.setDiscountAmount(BigDecimal.ZERO);
            serviceBill.setFinalAmount(estimate.getTotalPrice());
        }
        //todo: change status of estimate and service ticket
        serviceTicket.setTicketStatus(TicketStatus.COMPLETED);
        serviceTicketRepository.save(serviceTicket);
        estimate.setStatus(EstimateEnum.ARCHIVED);
        estimateRepository.save(estimate);
        serviceBill.setBillStatus(BillingStatus.DRAFT.name());
        ServiceBill saved = billingRepository.createNewBilling(serviceBill);
        return serviceBillDtoMapper.mapToDto(saved);
    }

    private Promotion resolvePromotion(ServiceBillDto serviceBillDto){
        if (serviceBillDto.getPromotionId() == null){return null;}
        return promotionRepo.getAllPromotionForBilling(serviceBillDto);
    }

    private void validateBillingRequest(Estimate estimate, ServiceTicketJpa serviceTicket) {
        if (serviceTicket == null) {
            throw new BillingException("Service Ticket not found!", BillingErrorCode.SERVICE_TICKET_404);
        }
        if (estimate == null) {
            throw new BillingException("Estimate not found!", BillingErrorCode.ESTIMATE_404);
        }
        if (serviceTicket.getTicketStatus() != TicketStatus.COMPLETED) {
            throw new BillingException("Service Ticket not done or wrong status!", BillingErrorCode.SERVICE_TICKET_STATUS_NOT_MATCH);
        }
        if (estimate.getStatus() != EstimateEnum.APPROVED) {
            throw new BillingException("Estimate not approved, wrong status!", BillingErrorCode.ESTIMATE_STATUS_NOT_MATCH);
        }
        if (!estimate.getServiceTicketId().equals(serviceTicket.getServiceTicketId())) {
            throw new BillingException("Service Ticket and Estimate not match", BillingErrorCode.ESTIMATE_NOT_MATCH_SERVICE_TICKET);
        }
    }

    public PaymentTransactionDto createNewPayment(PaymentTransactionDto dto) {
        PaymentTransaction paymentTransactionDto = serviceBillDtoMapper.mapPaymentToEntity(dto);
        paymentTransactionDto.setPaidAt(Instant.now());
        PaymentTransaction paymentTransaction = paymentTransationRepo.createNewPayment(paymentTransactionDto);
        ServiceBill serviceBill = billingRepository.getBillingByBillingId(dto.getBillId());
        ServiceTicketJpa serviceTicketJpa = serviceTicketRepository.findByServiceTicketId(serviceBill.getServiceTicketId());
        serviceTicketJpa.setTicketStatus(TicketStatus.PAID);
        serviceTicketRepository.save(serviceTicketJpa);
        return serviceBillDtoMapper.mapPaymentToDto(paymentTransaction);
    }
}
