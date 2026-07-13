package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.billing.api.dto.ServiceBillCreateDto;
import com.g42.platform.gms.billing.api.dto.ServiceBillDto;
import com.g42.platform.gms.billing.app.service.BillingService;
import com.g42.platform.gms.common.enums.CodePrefix;
import com.g42.platform.gms.common.enums.EstimateEnum;
import com.g42.platform.gms.estimation.api.internal.EstimateInternalApi;
import com.g42.platform.gms.estimation.app.service.EstimateService;
import com.g42.platform.gms.estimation.app.service.StockAllocationService;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.AssignStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.AvailableStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.parts_sale.PartsSaleCreateDto;
import com.g42.platform.gms.service_ticket_management.api.dto.parts_sale.PartsSaleTicketDto;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketType;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Luồng bán linh kiện rút gọn cho đại lý/garage khác.
 *
 * Tạo một phiếu dịch vụ PARTS_SALE trong MỘT transaction:
 * bỏ qua kiểm tra an toàn, trạng thái sửa xe và giao việc thủ công.
 * Các bước thanh toán / xuất kho / trả hàng phía sau dùng nguyên
 * luồng hiện có (BillingService, StockIssueService, ReturnEntryService).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartsSaleService {

    private static final String ADVISOR_ROLE = "ADVISOR";

    private final ServiceTicketRepo serviceTicketRepo;
    private final ServiceTicketCodeGenerator ticketCodeGenerator;
    private final TicketAssignmentService ticketAssignmentService;
    private final EstimateInternalApi estimateInternalApi;
    private final EstimateService estimateService;
    private final StockAllocationService stockAllocationService;
    private final BillingService billingService;

    /**
     * Tạo phiếu bán linh kiện từ báo giá DRAFT: tạo ticket (không booking,
     * không xe), gán nhân viên tạo phiếu, giữ hàng, archive báo giá và tạo bill.
     * Lỗi ở bất kỳ bước nào (vd: thiếu tồn kho) sẽ rollback toàn bộ.
     */
    @Transactional
    public PartsSaleTicketDto createPartsSale(PartsSaleCreateDto dto, Integer staffId) {
        if (dto.getCustomerId() == null) {
            throw new RuntimeException("Thiếu thông tin khách hàng (customerId)");
        }
        if (dto.getEstimateId() == null) {
            throw new RuntimeException("Thiếu báo giá (estimateId)");
        }

        Estimate estimate = estimateInternalApi.findById(dto.getEstimateId());
        if (estimate == null) {
            throw new RuntimeException("Không tìm thấy báo giá: " + dto.getEstimateId());
        }
        if (estimate.getStatus() != EstimateEnum.DRAFT) {
            throw new RuntimeException("Báo giá không ở trạng thái DRAFT (hiện tại: " + estimate.getStatus() + ")");
        }
        // Chặn tạo trùng phiếu từ cùng một báo giá (double-click)
        if (estimate.getServiceTicketId() != null) {
            throw new RuntimeException("Báo giá đã được gắn vào phiếu khác: " + estimate.getServiceTicketId());
        }

        // 1. Tạo phiếu PARTS_SALE: không booking, không xe, không kiểm tra an toàn
        ServiceTicket ticket = new ServiceTicket();
        ticket.setTicketCode(ticketCodeGenerator.generateCode(LocalDate.now(), CodePrefix.PARTS_SALE));
        ticket.setBookingId(null);
        ticket.setVehicleId(null);
        ticket.setCustomerId(dto.getCustomerId());
        ticket.setCreatedBy(staffId);
        ticket.setCustomerRequest(dto.getNote());
        ticket.setSafetyInspectionEnabled(false);
        ticket.setReceivedAt(LocalDateTime.now());
        ticket.initializeDefaults();
        ticket.setTicketType(TicketType.PARTS_SALE);
        ServiceTicket saved = serviceTicketRepo.save(ticket);
        log.info("Created parts-sale ticket {} (id={})", saved.getTicketCode(), saved.getServiceTicketId());

        // 2. Gắn báo giá vào phiếu
        estimateInternalApi.linkEstimateToServiceTicket(dto.getEstimateId(), saved.getServiceTicketId());

        // 3. Tự động gán nhân viên khi phiếu còn CREATED (assignStaff chặn phiếu COMPLETED)
        autoAssignAdvisor(saved.getServiceTicketId(), staffId);

        // 4. Giữ hàng (RESERVED) — thiếu tồn kho sẽ ném exception và rollback
        stockAllocationService.createStockAllocation(dto.getEstimateId(), staffId);

        // 5. Nhảy thẳng sang COMPLETED + archive báo giá để thỏa điều kiện tạo bill
        saved.setTicketStatus(TicketStatus.COMPLETED);
        saved.setCompletedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());
        saved = serviceTicketRepo.save(saved);
        estimateService.updateEstimateStatus(dto.getEstimateId(), EstimateEnum.ARCHIVED);

        // 6. Tạo bill (BillingService validate COMPLETED + ARCHIVED như luồng cũ)
        ServiceBillCreateDto billCreateDto = new ServiceBillCreateDto();
        billCreateDto.setServiceTicketId(saved.getServiceTicketId());
        billCreateDto.setEstimateId(dto.getEstimateId());
        billCreateDto.setPaymentStatus("UNPAID");
        ServiceBillDto bill = billingService.createNewBilling(billCreateDto);

        return new PartsSaleTicketDto(
                saved.getServiceTicketId(),
                saved.getTicketCode(),
                saved.getCustomerId(),
                dto.getEstimateId(),
                bill.getBillId(),
                bill.getFinalAmount()
        );
    }

    /**
     * Gán người tạo phiếu làm ADVISOR; nếu người tạo không có role ADVISOR
     * thì fallback sang advisor ít việc nhất; không có ai thì bỏ qua
     * (markAssignmentDone khi thanh toán chịu được phiếu không có assignment).
     */
    private void autoAssignAdvisor(Integer ticketId, Integer staffId) {
        try {
            AssignStaffDto assignDto = new AssignStaffDto();
            assignDto.setStaffId(staffId);
            assignDto.setRoleInTicket(ADVISOR_ROLE);
            assignDto.setNote("Tự động gán khi tạo phiếu bán linh kiện");
            ticketAssignmentService.assignStaff(ticketId, assignDto);
            return;
        } catch (Exception ex) {
            log.warn("Cannot assign creator (staffId={}) as advisor for parts-sale ticket {}: {}",
                    staffId, ticketId, ex.getMessage());
        }
        try {
            List<AvailableStaffDto> advisors = ticketAssignmentService.getAvailableStaff(ticketId, ADVISOR_ROLE);
            if (advisors != null && !advisors.isEmpty()) {
                AssignStaffDto fallback = new AssignStaffDto();
                fallback.setStaffId(advisors.get(0).getStaffId());
                fallback.setRoleInTicket(ADVISOR_ROLE);
                fallback.setNote("Tự động gán advisor ít việc nhất cho phiếu bán linh kiện");
                ticketAssignmentService.assignStaff(ticketId, fallback);
            } else {
                log.warn("No advisor available to auto-assign for parts-sale ticket {}", ticketId);
            }
        } catch (Exception ex) {
            log.warn("Fallback advisor assignment failed for parts-sale ticket {}: {}", ticketId, ex.getMessage());
        }
    }
}
