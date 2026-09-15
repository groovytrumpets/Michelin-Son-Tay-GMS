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
import com.g42.platform.gms.estimation.api.internal.EstimateInternalApi;
import com.g42.platform.gms.estimation.app.service.EstimateService;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.repository.EstimateRepository;
import com.g42.platform.gms.notification.application.service.CustomerNotificationDispatcher;
import com.g42.platform.gms.notification.domain.NotificationRecipient;
import com.g42.platform.gms.promotion.domain.entity.Promotion;
import com.g42.platform.gms.promotion.domain.repository.PromotionRepo;
import com.g42.platform.gms.service_ticket_management.api.internal.ServiceTicketInternalApi;
import com.g42.platform.gms.service_ticket_management.application.service.ServiceTicketManageService;
import com.g42.platform.gms.service_ticket_management.application.service.TicketAssignmentService;
import com.g42.platform.gms.service_ticket_management.domain.entity.BackfillGuard;
import com.g42.platform.gms.service_ticket_management.domain.enums.EntryMode;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketType;
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
    private CustomerNotificationDispatcher notificationDispatcher;
    @Autowired
    @Qualifier("warehouseStockAllocationService")
    private com.g42.platform.gms.warehouse.app.service.allocation.StockAllocationService warehouseStockAllocationService;
    @Autowired
    private EstimateInternalApi estimateInternalApi;

    //todo: get available promotion
    @Transactional
    public ServiceBillDto createNewBilling(ServiceBillCreateDto serviceBillDto) {
        ServiceBill serviceBill = serviceBillDtoMapper.mapToCreateEntity(serviceBillDto);
        //todo: check estimate match serviceTicket
        Estimate estimate = estimateRepository.findEstimateByServiceIdAndLatestVerson(serviceBillDto.getServiceTicketId());
        System.out.println("DEBUG: Estimate: " + estimate.getId());
        ServiceTicketJpa serviceTicket = serviceTicketRepository.findByServiceTicketId(serviceBillDto.getServiceTicketId());
        if (serviceTicket != null) {
            BackfillGuard.requireNotUnapprovedBackfill(serviceTicket.getEntryMode(),
                    serviceTicket.getBackfillReviewStatus(), serviceTicket.getTicketCode());
        }
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

    /**
     * Hoa don cua mot phieu dich vu, hoac null neu chua co.
     * Khac getBillWithEstimate(...) o cho khong nem exception khi chua co hoa don,
     * de goi ben chot phieu ban linh kien co the kiem tra double-click.
     */
    public ServiceBillDto findBillByServiceTicket(Integer serviceTicketId) {
        if (serviceTicketId == null) {
            return null;
        }
        ServiceBill bill = billingRepository.getBillingByServiceTicket(serviceTicketId);
        return bill == null ? null : serviceBillDtoMapper.mapToDto(bill);
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
        ServiceTicketJpa ticketToPay = serviceTicketRepository.findByServiceTicketId(serviceBill.getServiceTicketId());
        if (ticketToPay != null) {
            // Phiếu nhập bù ghi thanh toán qua recordBackfillPayment lúc quản lý duyệt, không thu tiền ở quầy
            if (ticketToPay.getEntryMode() == EntryMode.BACKFILL) {
                throw new IllegalArgumentException("Phiếu nhập bù " + ticketToPay.getTicketCode()
                        + " được ghi thanh toán khi quản lý duyệt, không thu tiền lại ở màn thanh toán.");
            }
        }
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

        // Phiếu dịch vụ thường: chỉ chuyển ticket sang PAID, phiếu xuất kho DRAFT
        // do nhân viên kho tạo qua API yêu cầu xuất kho từ stock allocation.
        //
        // Phiếu bán linh kiện: khách trả tiền là lấy hàng đi luôn, không có khâu kho
        // duyệt riêng — nên xuất kho ngay trong transaction thanh toán này. Không làm
        // vậy thì allocation kẹt RESERVED vĩnh viễn và tồn kho không bao giờ giảm.
        if (serviceTicketJpa.getTicketType() == TicketType.PARTS_SALE) {
            warehouseStockAllocationService.issueAndConfirmOnPaid(serviceBill.getServiceTicketId(), staffId);
        }

        //todo: send feedback
        String code = serviceTicketInternalApi.getCodeByServiceTicketId(serviceBill.getServiceTicketId());
        String phone = customerInternalApi.getCustomerPhoneByServiceTicketId(serviceBill.getServiceTicketId());
        String name = customerInternalApi.getNameByServiceTicketId(serviceBill.getServiceTicketId());

        if (phone!=null) {
            com.g42.platform.gms.auth.entity.CustomerProfile feedbackCustomer =
                    customerInternalApi.findById(serviceTicketJpa.getCustomerId());
            NotificationRecipient recipient = feedbackCustomer != null
                    ? NotificationRecipient.of(phone, feedbackCustomer.getEmail(), feedbackCustomer.getNotificationChannel())
                    : NotificationRecipient.phoneOnly(phone);
            notificationDispatcher.sendFeedback(recipient, name, code);
        }
        billingRepository.save(serviceBill);
        
        // Reward referrer if this is the customer's first paid ticket
        try {
            com.g42.platform.gms.auth.entity.CustomerProfile customer = customerInternalApi.findById(serviceTicketJpa.getCustomerId());
            if (customer != null && customer.getReferrerId() != null) {
                long paidCount = serviceTicketRepository.countByCustomerIdAndTicketStatus(customer.getCustomerId(), TicketStatus.PAID);
                if (paidCount == 1) { // 1 means this newly paid ticket is the first one
                    customerInternalApi.adjustPoints(customer.getReferrerId(), 50, "Thưởng giới thiệu khách hàng: " + customer.getPhone() + " (Đã hoàn thành dịch vụ đầu tiên)");
                }
            }
        } catch (Exception e) {
            // Ignore reward error
        }

        //todo: change status of assignment

        ticketAssignmentService.markAssignmentDone(serviceBill.getServiceTicketId());

        //todo: calculate gross-profit
//        estimateInternalApi.calculateAndLockGrossProfit(serviceBill.getServiceTicketId());
        return serviceBillDtoMapper.mapPaymentToDto(paymentTransaction);
    }

    /**
     * Ghi thanh toán cho phiếu nhập bù lúc quản lý duyệt (TicketBackfillService.approve).
     *
     * Khác createNewPayment ở chỗ: tiền đã thu từ ngày thực tế nên paid_at/delivered_at
     * lấy theo ngày đó (báo cáo doanh thu rơi đúng ngày), và KHÔNG gửi thông báo mời
     * đánh giá hay thưởng người giới thiệu — khách đã về từ hôm trước, nhắn lúc này
     * chỉ gây khó hiểu. Luôn xuất kho ngay (cả phiếu sửa xe), vì hàng thực tế đã rời
     * kho từ hôm đó, không còn khâu kho duyệt phiếu xuất nữa.
     */
    @Transactional
    public PaymentTransactionDto recordBackfillPayment(Integer billId, String method,
                                                       LocalDateTime paidAt, Integer staffId) {
        ServiceBill serviceBill = billingRepository.getBillingByBillingId(billId);
        if (serviceBill == null) {
            throw new BillingException("Bill not found!", BillingErrorCode.ESTIMATE_404);
        }
        Instant paidInstant = paidAt.atZone(java.time.ZoneId.systemDefault()).toInstant();

        PaymentTransactionDto dto = new PaymentTransactionDto();
        dto.setBillId(billId);
        dto.setMethod(method);
        PaymentTransaction payment = serviceBillDtoMapper.mapPaymentToEntity(dto);
        payment.setPaidAt(paidInstant);
        payment.setAmount(serviceBill.getFinalAmount());
        PaymentTransaction saved = paymentTransationRepo.createNewPayment(payment);

        ServiceTicketJpa ticket = serviceTicketRepository.findByServiceTicketId(serviceBill.getServiceTicketId());
        ticket.setTicketStatus(TicketStatus.PAID);
        ticket.setDeliveredAt(paidAt);
        serviceTicketRepository.save(ticket);

        serviceBill.setPaymentStatus(PaymentStatus.PAID.name());
        serviceBill.setPaidAt(paidInstant);
        billingRepository.save(serviceBill);

        warehouseStockAllocationService.issueAndConfirmOnPaid(serviceBill.getServiceTicketId(), staffId);
        ticketAssignmentService.markAssignmentDone(serviceBill.getServiceTicketId());
        return serviceBillDtoMapper.mapPaymentToDto(saved);
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
