package com.g42.platform.gms.billing.app.service;

import com.g42.platform.gms.auth.api.internal.CustomerInternalApi;
import com.g42.platform.gms.billing.api.dto.BillEstimateDto;
import com.g42.platform.gms.billing.api.dto.PaymentTransactionDto;
import com.g42.platform.gms.billing.api.dto.ServiceBillCreateDto;
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
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.common.enums.EstimateEnum;
import com.g42.platform.gms.customer.domain.entity.CustomerProfile;
import com.g42.platform.gms.estimation.api.dto.EstimateRespondDto;
import com.g42.platform.gms.estimation.app.service.EstimateService;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.repository.EstimateRepository;
import com.g42.platform.gms.notification.infrastructure.ZaloNotificationSender;
import com.g42.platform.gms.promotion.domain.entity.Promotion;
import com.g42.platform.gms.promotion.domain.repository.PromotionRepo;
import com.g42.platform.gms.service_ticket_management.api.internal.ServiceTicketInternalApi;
import com.g42.platform.gms.service_ticket_management.application.service.ServiceTicketManageService;
import com.g42.platform.gms.service_ticket_management.application.service.TicketAssignmentService;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @Autowired
    private ServiceTicketManageService serviceTicketManageService;
    @Autowired
    private EstimateService estimateService;
    @Autowired
    private TicketAssignmentService ticketAssignmentService;
    @Autowired
    private ServiceTicketInternalApi serviceTicketInternalApi;
    @Autowired
    private CustomerInternalApi customerInternalApi;
    @Autowired
    private ZaloNotificationSender zaloNotificationSender;
    @Autowired
    @Qualifier("warehouseStockAllocationService")
    private com.g42.platform.gms.warehouse.app.service.allocation.StockAllocationService warehouseStockAllocationService;

    //todo: get available promotion
    @Transactional
    public ServiceBillDto createNewBilling(ServiceBillCreateDto serviceBillDto) {
        ServiceBill serviceBill = serviceBillDtoMapper.mapToCreateEntity(serviceBillDto);
        //todo: check estimate match serviceTicket
        Estimate estimate = estimateRepository.findEstimateByServiceIdAndLatestVerson(serviceBillDto.getServiceTicketId());
        System.out.println("DEBUG: Estimate: " + estimate.getId());
        ServiceTicketJpa serviceTicket = serviceTicketRepository.findByServiceTicketId(serviceBillDto.getServiceTicketId());
        validateBillingRequest(estimate, serviceTicket);
        serviceBill.setSubTotal(estimate.getTotalPrice());
        serviceBill.setEstimateId(estimate.getId());
        //todo: check promotion available for billing
//        Promotion promotion = resolvePromotion(serviceBillDto);
//        if (promotion != null) {
//        System.out.println("DEBUG: Promotion: " + promotion.getPromotionId());
//            System.out.println("DEBUG: Promotion: " + promotion.getDiscountPercent());
//            BigDecimal baseAmount = estimate.getTotalPrice();
//            BigDecimal discountAmount = baseAmount
//                    .multiply(promotion.getDiscountPercent())
//                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
//            serviceBill.setDiscountAmount(discountAmount);
//            System.out.println("DEBUG: Promotion: " + discountAmount);
//            promotionRepo.countUsed(promotion.getPromotionId());
//            serviceBill.setFinalAmount(estimate.getTotalPrice().subtract(discountAmount));
//        }else {
            serviceBill.setDiscountAmount(BigDecimal.ZERO);
            serviceBill.setFinalAmount(estimate.getTotalPrice());
//            throw new BillingException("Đơn hàng chưa đủ điều kiện áp dụng Promotion",BillingErrorCode.PROMOTION404);
//        }
        //todo: change status of estimate and service ticket
        serviceTicket.setTicketStatus(TicketStatus.COMPLETED);
        serviceTicketRepository.save(serviceTicket);
        estimate.setStatus(EstimateEnum.ARCHIVED);
        estimateRepository.save(estimate);
//        serviceBill.setPaidAt(Instant.now());
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
        if (estimate.getStatus() != EstimateEnum.ARCHIVED) {
            throw new BillingException("Estimate not approved, wrong status!", BillingErrorCode.ESTIMATE_STATUS_NOT_MATCH);
        }
        if (!estimate.getServiceTicketId().equals(serviceTicket.getServiceTicketId())) {
            throw new BillingException("Service Ticket and Estimate not match", BillingErrorCode.ESTIMATE_NOT_MATCH_SERVICE_TICKET);
        }
    }
    @Transactional
    public PaymentTransactionDto createNewPayment(PaymentTransactionDto dto, Integer staffId) {
        ServiceBill serviceBill = billingRepository.getBillingByBillingId(dto.getBillId());
        PaymentTransaction paymentTransactionDto = serviceBillDtoMapper.mapPaymentToEntity(dto);
        paymentTransactionDto.setPaidAt(Instant.now());
        paymentTransactionDto.setAmount(serviceBill.getFinalAmount());
        PaymentTransaction paymentTransaction = paymentTransationRepo.createNewPayment(paymentTransactionDto);
        ServiceTicketJpa serviceTicketJpa = serviceTicketRepository.findByServiceTicketId(serviceBill.getServiceTicketId());
        serviceTicketJpa.setTicketStatus(TicketStatus.PAID);
        serviceTicketJpa.setDeliveredAt(LocalDateTime.now());
        serviceTicketRepository.save(serviceTicketJpa);
        serviceBill.setPaymentStatus(PaymentStatus.PAID.name());
        serviceBill.setPaidAt(Instant.now());

        // Luong moi: chi chuyen ticket sang PAID, viec tao phieu xuat kho DRAFT
        // duoc goi bang API yeu cau xuat kho tu stock allocation.

        //todo: send feedback
        String code = serviceTicketInternalApi.getCodeByServiceTicketId(serviceBill.getServiceTicketId());
        String phone = customerInternalApi.getCustomerPhoneByServiceTicketId(serviceBill.getServiceTicketId());
        String name = customerInternalApi.getNameByServiceTicketId(serviceBill.getServiceTicketId());

        if (phone!=null) {
        zaloNotificationSender.sendFeedback(phone,name,code);
        }
        billingRepository.save(serviceBill);
        //todo: change status of assignment

        ticketAssignmentService.markAssignmentDone(serviceBill.getServiceTicketId());
        return serviceBillDtoMapper.mapPaymentToDto(paymentTransaction);
    }

    public BillEstimateDto getBillWithEstimate(Integer serviceTicketId) {
        BillEstimateDto billEstimateDto = new BillEstimateDto();
        ServiceBill serviceBill = billingRepository.getBillingByServiceTicket(serviceTicketId);
        if (serviceBill == null) {
            throw new BillingException("Bill not found!", BillingErrorCode.ESTIMATE_404);
        }
        billEstimateDto = serviceBillDtoMapper.toBillEstimateDto(serviceBill);
        List<EstimateRespondDto> estimateRespondDtos = estimateService.getEstimateByCode(serviceTicketId);
        if (estimateRespondDtos!=null||billEstimateDto!=null) {

        billEstimateDto.setEstimate(estimateRespondDtos);
        }
        return  billEstimateDto;
    }
}
