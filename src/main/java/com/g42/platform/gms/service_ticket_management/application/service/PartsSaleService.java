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
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.domain.enums.BookingStatus;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Luồng bán linh kiện rút gọn cho đại lý/garage khác.
 *
 * Vòng đời phiếu PARTS_SALE:
 *   HOLDING   — lưu báo giá xong: phiếu đã có mặt ở /parts-sale-ticket-management,
 *               hàng đã được giữ (allocation RESERVED), chưa có hoá đơn
 *   COMPLETED — bấm thanh toán: báo giá ARCHIVED + sinh hoá đơn chờ thu tiền
 *   PAID      — thu ngân thu tiền: BillingService xuất kho thật (trừ tồn, trừ lô,
 *               allocation RESERVED thành COMMITTED) rồi chuyển phiếu sang PAID
 *   CANCELLED — huỷ phiếu (thủ công hoặc job quét quá hạn): nhả hàng về kho
 *
 * Phiếu bỏ qua kiểm tra an toàn, trạng thái sửa xe và giao việc thủ công.
 * Bước trả hàng phía sau dùng nguyên luồng hiện có (ReturnEntryService).
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
    private final BookingRepository bookingRepository;

    // Field injection: @Qualifier khong duoc Lombok copy sang constructor (repo khong co lombok.config)
    @Autowired
    @Qualifier("warehouseStockAllocationService")
    private com.g42.platform.gms.warehouse.app.service.allocation.StockAllocationService warehouseStockAllocationService;

    /**
     * Giữ hàng cho một báo giá bán linh kiện.
     *
     * Gọi khi nhân viên bấm "Lưu báo giá" ở /parts-sales: giữ chỗ hàng đồng nghĩa
     * với việc đã có một đơn bán thật, nên phiếu PARTS_SALE được tạo ngay ở trạng
     * thái HOLDING để nó hiện ở /parts-sale-ticket-management thay vì nằm ẩn.
     *
     * Hàm idempotent theo báo giá: bấm lưu nhiều lần, hoặc lưu lại sau khi sửa báo
     * giá (sinh version mới), đều dùng lại đúng phiếu cũ — bản báo giá cũ được nhả
     * hàng trước rồi giữ lại theo bản mới nên tồn kho không bị giữ chồng.
     *
     * Thiếu tồn kho sẽ ném exception và rollback toàn bộ.
     */
    @Transactional
    public PartsSaleTicketDto holdPartsSale(PartsSaleCreateDto dto, Integer staffId) {
        Estimate estimate = requireDraftEstimate(dto);

        ServiceTicket ticket = resolveHoldingTicket(dto, estimate, staffId);
        Integer ticketId = ticket.getServiceTicketId();

        // Báo giá được sửa (version mới) thì nhả hàng đã giữ theo bản cũ của chính phiếu này
        warehouseStockAllocationService.releaseOtherEstimates(ticketId, dto.getEstimateId(), staffId);

        // Gắn báo giá vào phiếu (bản mới của cùng phiếu cũng phải trỏ về đây)
        if (!ticketId.equals(estimate.getServiceTicketId())) {
            estimateInternalApi.linkEstimateToServiceTicket(dto.getEstimateId(), ticketId);
        }

        // Giữ hàng (RESERVED) — createStockAllocation tự bỏ qua nếu báo giá đã có allocation
        stockAllocationService.createStockAllocation(dto.getEstimateId(), staffId);

        return new PartsSaleTicketDto(
                ticketId,
                ticket.getTicketCode(),
                ticket.getCustomerId(),
                dto.getEstimateId(),
                null,
                null,
                ticket.getTicketStatus().name()
        );
    }

    /**
     * Chốt phiếu bán linh kiện để chuyển sang màn thu ngân.
     *
     * Dùng lại phiếu HOLDING đã tạo lúc lưu báo giá (tự tạo nếu FE gọi thẳng vào
     * đây mà chưa qua bước giữ hàng), archive báo giá và sinh hoá đơn UNPAID.
     * Hàng vẫn đang ở trạng thái RESERVED; việc trừ tồn thật xảy ra khi thu tiền
     * (BillingService.createNewPayment gọi issueAndConfirmOnPaid).
     */
    @Transactional
    public PartsSaleTicketDto createPartsSale(PartsSaleCreateDto dto, Integer staffId) {
        // Đã chốt rồi (double-click / gọi lại): trả về hoá đơn cũ, không sinh hoá đơn thứ hai.
        // Phải kiểm tra TRƯỚC bước giữ hàng vì lúc đó báo giá đã ARCHIVED, không còn DRAFT.
        PartsSaleTicketDto alreadyBilled = findAlreadyBilled(dto);
        if (alreadyBilled != null) {
            return alreadyBilled;
        }

        // Gọi lại (hoặc lần đầu) bước giữ hàng — an toàn với double-click
        PartsSaleTicketDto held = holdPartsSale(dto, staffId);
        ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(held.getServiceTicketId());

        // Nhảy sang COMPLETED + archive báo giá để thỏa điều kiện tạo bill
        ticket.setTicketStatus(TicketStatus.COMPLETED);
        ticket.setCompletedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket = serviceTicketRepo.save(ticket);
        estimateService.updateEstimateStatus(dto.getEstimateId(), EstimateEnum.ARCHIVED);

        ServiceBillCreateDto billCreateDto = new ServiceBillCreateDto();
        billCreateDto.setServiceTicketId(ticket.getServiceTicketId());
        billCreateDto.setEstimateId(dto.getEstimateId());
        billCreateDto.setPaymentStatus("UNPAID");
        ServiceBillDto bill = billingService.createNewBilling(billCreateDto);

        return new PartsSaleTicketDto(
                ticket.getServiceTicketId(),
                ticket.getTicketCode(),
                ticket.getCustomerId(),
                dto.getEstimateId(),
                bill.getBillId(),
                bill.getFinalAmount(),
                ticket.getTicketStatus().name()
        );
    }

    /**
     * Chốt một phiếu đang giữ hàng đã tạo từ trước (mở lại từ màn quản lý phiếu bán).
     *
     * Nhân viên lưu báo giá xong có thể rời trang; phiếu HOLDING vẫn nằm ở
     * /parts-sale-ticket-management. Hàm này lấy bản báo giá mới nhất của phiếu
     * rồi chạy đúng luồng chốt như bấm thanh toán ở /parts-sales.
     */
    @Transactional
    public PartsSaleTicketDto checkoutHoldingTicket(Integer serviceTicketId, Integer staffId) {
        ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(serviceTicketId);
        if (ticket == null) {
            throw new RuntimeException("Không tìm thấy phiếu: " + serviceTicketId);
        }
        if (ticket.getTicketType() != TicketType.PARTS_SALE) {
            throw new RuntimeException("Phiếu " + ticket.getTicketCode() + " không phải phiếu bán linh kiện");
        }

        Estimate estimate = estimateInternalApi.findLatestByServiceTicketId(serviceTicketId);
        if (estimate == null) {
            throw new RuntimeException("Phiếu " + ticket.getTicketCode() + " chưa có báo giá");
        }

        PartsSaleCreateDto dto = new PartsSaleCreateDto();
        dto.setCustomerId(ticket.getCustomerId());
        dto.setEstimateId(estimate.getId());
        dto.setNote(ticket.getCustomerRequest());
        return createPartsSale(dto, staffId);
    }

    /**
     * Huỷ phiếu bán linh kiện đang giữ hàng và nhả hàng về kho.
     *
     * Chỉ áp dụng cho phiếu chưa chốt (HOLDING). Phiếu đã có hoá đơn phải đi
     * đường trả hàng (ReturnEntryService) chứ không huỷ trắng như thế này.
     */
    @Transactional
    public void cancelPartsSale(Integer serviceTicketId, Integer staffId) {
        ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(serviceTicketId);
        if (ticket == null) {
            throw new RuntimeException("Không tìm thấy phiếu: " + serviceTicketId);
        }
        if (ticket.getTicketType() != TicketType.PARTS_SALE) {
            throw new RuntimeException("Phiếu " + ticket.getTicketCode() + " không phải phiếu bán linh kiện");
        }
        if (ticket.getTicketStatus() == TicketStatus.CANCELLED) {
            return;
        }
        if (ticket.getTicketStatus() != TicketStatus.HOLDING) {
            throw new RuntimeException("Chỉ huỷ được phiếu đang giữ hàng. Hiện tại: " + ticket.getTicketStatus());
        }

        releaseHold(ticket, staffId);
    }

    /**
     * Nhả hàng của các phiếu giữ quá hạn (job chạy nền gọi vào).
     *
     * @param holdExpiredBefore mốc thời gian: phiếu HOLDING tạo trước mốc này bị huỷ
     * @return số phiếu đã nhả
     */
    @Transactional
    public int releaseExpiredHolds(LocalDateTime holdExpiredBefore) {
        List<ServiceTicket> expired = serviceTicketRepo.findExpiredHoldingTickets(
                TicketType.PARTS_SALE, holdExpiredBefore);
        int released = 0;
        for (ServiceTicket ticket : expired) {
            try {
                releaseHold(ticket, ticket.getCreatedBy());
                released++;
            } catch (Exception ex) {
                log.error("Không nhả được hàng của phiếu giữ quá hạn {}: {}",
                        ticket.getTicketCode(), ex.getMessage());
            }
        }
        return released;
    }

    /** Nhả allocation + đóng phiếu + huỷ báo giá gắn với phiếu. */
    private void releaseHold(ServiceTicket ticket, Integer staffId) {
        warehouseStockAllocationService.release(ticket.getServiceTicketId(), staffId);

        Estimate estimate = estimateInternalApi.findLatestByServiceTicketId(ticket.getServiceTicketId());
        if (estimate != null && estimate.getStatus() != EstimateEnum.ARCHIVED) {
            estimateService.updateEstimateStatus(estimate.getId(), EstimateEnum.CANCELLED);
        }

        ticket.setTicketStatus(TicketStatus.CANCELLED);
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);
        log.info("Released parts-sale hold for ticket {}", ticket.getTicketCode());
    }

    /**
     * Hoá đơn đã tạo cho báo giá này, hoặc null nếu chưa chốt.
     * Dùng để lần bấm thanh toán thứ hai trả về đúng hoá đơn cũ thay vì báo lỗi
     * "báo giá không ở trạng thái DRAFT".
     */
    private PartsSaleTicketDto findAlreadyBilled(PartsSaleCreateDto dto) {
        if (dto.getEstimateId() == null) {
            return null;
        }
        Estimate estimate = estimateInternalApi.findById(dto.getEstimateId());
        if (estimate == null || estimate.getServiceTicketId() == null) {
            return null;
        }
        ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(estimate.getServiceTicketId());
        if (ticket == null) {
            return null;
        }
        ServiceBillDto bill = billingService.findBillByServiceTicket(ticket.getServiceTicketId());
        if (bill == null) {
            return null;
        }
        return new PartsSaleTicketDto(
                ticket.getServiceTicketId(),
                ticket.getTicketCode(),
                ticket.getCustomerId(),
                dto.getEstimateId(),
                bill.getBillId(),
                bill.getFinalAmount(),
                ticket.getTicketStatus().name()
        );
    }

    private Estimate requireDraftEstimate(PartsSaleCreateDto dto) {
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
        return estimate;
    }

    /**
     * Tìm phiếu giữ hàng đang mở cho báo giá này, hoặc tạo mới nếu chưa có.
     * Báo giá đã gắn vào một phiếu đã thu tiền/huỷ thì không cho dùng lại.
     */
    private ServiceTicket resolveHoldingTicket(PartsSaleCreateDto dto, Estimate estimate, Integer staffId) {
        // Ưu tiên phiếu FE gửi lên (màn bán hàng giữ lại id sau lần giữ hàng đầu tiên),
        // rồi tới phiếu báo giá đang trỏ vào, cuối cùng lần ngược chuỗi version cha —
        // sửa báo giá sinh version mới chưa trỏ vào phiếu nào, không lần ngược thì
        // mỗi lần sửa lại đẻ thêm một phiếu bán hàng.
        Integer linkedTicketId = dto.getServiceTicketId() != null
                ? dto.getServiceTicketId()
                : estimate.getServiceTicketId();
        if (linkedTicketId == null) {
            linkedTicketId = findTicketIdFromPreviousVersions(estimate);
        }

        if (linkedTicketId != null) {
            ServiceTicket linked = serviceTicketRepo.findByServiceTicketId(linkedTicketId);
            if (linked != null) {
                if (linked.getTicketStatus() == TicketStatus.HOLDING
                        || linked.getTicketStatus() == TicketStatus.COMPLETED) {
                    attachBookingIfNeeded(linked, dto.getBookingId());
                    return linked;
                }
                throw new RuntimeException("Báo giá đã gắn vào phiếu " + linked.getTicketCode()
                        + " (trạng thái " + linked.getTicketStatus() + ")");
            }
        }
        return createHoldingTicket(dto, staffId);
    }

    /** Lần ngược chuỗi version báo giá để tìm phiếu đã gắn (nếu có). */
    private Integer findTicketIdFromPreviousVersions(Estimate estimate) {
        Integer previousId = estimate.getRevisedFromId();
        int guard = 0;
        while (previousId != null && guard++ < 50) {
            Estimate previous = estimateInternalApi.findById(previousId);
            if (previous == null) {
                return null;
            }
            if (previous.getServiceTicketId() != null) {
                return previous.getServiceTicketId();
            }
            previousId = previous.getRevisedFromId();
        }
        return null;
    }

    /**
     * Phiếu giữ hàng tạo ở /parts-sales trước, khách hẹn lấy sau: lúc đó booking mới
     * có, gắn vào phiếu đang giữ và đóng lịch lại thay vì tạo phiếu mới.
     */
    private void attachBookingIfNeeded(ServiceTicket ticket, Integer bookingId) {
        if (bookingId == null || ticket.getBookingId() != null) {
            return;
        }
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return;
        }
        booking.setStatus(BookingStatus.DONE);
        bookingRepository.save(booking);
        ticket.setBookingId(bookingId);
        serviceTicketRepo.save(ticket);
    }

    private ServiceTicket createHoldingTicket(PartsSaleCreateDto dto, Integer staffId) {
        ServiceTicket ticket = new ServiceTicket();
        String ticketCode;
        if (dto.getBookingId() != null) {
            Booking booking = bookingRepository.findById(dto.getBookingId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy booking: " + dto.getBookingId()));
            booking.setStatus(BookingStatus.DONE);
            bookingRepository.save(booking);
            ticketCode = booking.getBookingCode();
            ticket.setBookingId(dto.getBookingId());
        } else {
            ticketCode = ticketCodeGenerator.generateCode(LocalDate.now(), CodePrefix.PARTS_SALE);
            ticket.setBookingId(null);
        }
        ticket.setTicketCode(ticketCode);
        ticket.setVehicleId(null);
        ticket.setCustomerId(dto.getCustomerId());
        ticket.setCreatedBy(staffId);
        ticket.setCustomerRequest(dto.getNote());
        ticket.setSafetyInspectionEnabled(false);
        ticket.setReceivedAt(LocalDateTime.now());
        ticket.initializeDefaults();
        ticket.setTicketType(TicketType.PARTS_SALE);
        ticket.setTicketStatus(TicketStatus.HOLDING);
        ServiceTicket saved = serviceTicketRepo.save(ticket);
        log.info("Created parts-sale holding ticket {} (id={})", saved.getTicketCode(), saved.getServiceTicketId());

        autoAssignAdvisor(saved.getServiceTicketId(), staffId);
        return saved;
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
