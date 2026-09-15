package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.billing.api.dto.PaymentProofCreateDto;
import com.g42.platform.gms.billing.api.dto.ServiceBillCreateDto;
import com.g42.platform.gms.billing.api.dto.ServiceBillDto;
import com.g42.platform.gms.billing.app.service.BillingService;
import com.g42.platform.gms.billing.app.service.PaymentProofService;
import com.g42.platform.gms.billing.infrastructure.repository.PaymentProofJpaRepo;
import com.g42.platform.gms.common.enums.CodePrefix;
import com.g42.platform.gms.common.enums.EstimateEnum;
import com.g42.platform.gms.customer.domain.enums.CustomerType;
import com.g42.platform.gms.estimation.api.internal.EstimateInternalApi;
import com.g42.platform.gms.estimation.app.service.EstimateService;
import com.g42.platform.gms.estimation.app.service.StockAllocationService;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.notification.domain.NotificationChannel;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.AssignStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.backfill.BackfillCreateRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.backfill.BackfillDuplicateDto;
import com.g42.platform.gms.service_ticket_management.api.dto.backfill.BackfillParentDto;
import com.g42.platform.gms.service_ticket_management.api.dto.backfill.BackfillPolicyDto;
import com.g42.platform.gms.service_ticket_management.api.dto.backfill.BackfillStaffStatDto;
import com.g42.platform.gms.service_ticket_management.api.dto.backfill.BackfillTicketDto;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.enums.BackfillKind;
import com.g42.platform.gms.service_ticket_management.domain.enums.BackfillReviewStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.EntryMode;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketType;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import com.g42.platform.gms.staff.profile.infrastructure.repository.StaffProileJpaRepo;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import com.g42.platform.gms.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Nhập bù phiếu của ngày trước cho /create-booking (phiếu sửa xe) và /parts-sales (phiếu bán hàng).
 *
 * Khác /customer-legacy-import (chỉ ghi lịch sử, không đụng kho, không duyệt), phiếu nhập bù
 * là phiếu thật đi đúng vòng đời, nhưng có thêm hàng rào chống phiếu khống:
 *
 *   1. Nhân viên nhập bù  → phiếu HOLDING + backfill_review_status = PENDING_REVIEW.
 *      Hàng được GIỮ ngay (tồn khả dụng giảm, không bán trùng món thực tế đã rời kho),
 *      chứng từ (≥ 1 ảnh) lưu cùng transaction. Ngày thực tế chỉ được lùi trong giới hạn
 *      theo vai trò; received_at = ngày thực tế, created_at = lúc bấm nhập.
 *   2. Quản lý duyệt       → phân công NV đã làm, sinh hoá đơn, ghi thanh toán với paid_at =
 *      ngày thực tế, xuất kho thật. Người nhập không tự duyệt được phiếu của mình (trừ ADMIN).
 *   3. Quản lý từ chối     → nhả hàng về kho, huỷ báo giá + phiếu, giữ lại lý do.
 *
 * Trong lúc chờ duyệt, BackfillGuard chặn mọi luồng cũ (chốt phiếu, thu tiền, đổi trạng thái).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketBackfillService {

    /** Lễ tân/nhân viên thường chỉ được lùi 3 ngày; quản lý/admin tới 30 ngày. */
    static final int STAFF_MAX_DAYS_BACK = 3;
    static final int REVIEWER_MAX_DAYS_BACK = 30;
    /** Cho phép lệch đồng hồ máy trạm vài phút khi chọn "ngày giờ thực tế". */
    private static final long FUTURE_TOLERANCE_MINUTES = 5;
    private static final int REASON_MAX_LENGTH = 500;
    private static final Set<String> PAYMENT_METHODS = Set.of("CASH", "TRANSFER");
    private static final String ROLE_MANAGER = "ROLE_MANAGER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final ServiceTicketRepo serviceTicketRepo;
    private final ServiceTicketRepository serviceTicketJpaRepo;
    private final ServiceTicketCodeGenerator ticketCodeGenerator;
    private final TicketAssignmentService ticketAssignmentService;
    private final EstimateInternalApi estimateInternalApi;
    private final EstimateService estimateService;
    private final StockAllocationService stockAllocationService;
    private final BillingService billingService;
    private final PaymentProofService paymentProofService;
    private final PaymentProofJpaRepo paymentProofJpaRepo;
    private final CustomerProfileRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final StaffProileJpaRepo staffProfileRepo;

    // Field injection: @Qualifier khong duoc Lombok copy sang constructor (repo khong co lombok.config)
    @Autowired
    @Qualifier("warehouseStockAllocationService")
    private com.g42.platform.gms.warehouse.app.service.allocation.StockAllocationService warehouseStockAllocationService;

    // =====================================================================
    // Nhập bù
    // =====================================================================

    @Transactional
    public BackfillTicketDto create(BackfillCreateRequest req, StaffPrincipal principal) {
        Integer staffId = requireStaffId(principal);
        if (req == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu phiếu nhập bù.");
        }

        BackfillKind kind = parseEnum(BackfillKind.class, req.getKind(), "Loại nhập bù");
        String reason = trimToNull(req.getReason());
        if (reason == null) {
            throw new IllegalArgumentException("Vui lòng ghi lý do nhập bù.");
        }
        if (reason.length() > REASON_MAX_LENGTH) {
            throw new IllegalArgumentException("Lý do nhập bù tối đa " + REASON_MAX_LENGTH + " ký tự.");
        }
        String paymentMethod = trimToNull(req.getPaymentMethod());
        paymentMethod = paymentMethod == null ? null : paymentMethod.toUpperCase(Locale.ROOT);
        if (paymentMethod == null || !PAYMENT_METHODS.contains(paymentMethod)) {
            throw new IllegalArgumentException("Vui lòng chọn hình thức khách đã thanh toán (tiền mặt/chuyển khoản).");
        }
        List<PaymentProofCreateDto.Item> proofs = req.getProofs() == null ? List.of() : req.getProofs();
        if (proofs.isEmpty()) {
            throw new IllegalArgumentException("Phiếu nhập bù bắt buộc có ít nhất 1 ảnh chứng từ (phiếu viết tay, ảnh chuyển khoản...).");
        }

        LocalDateTime actualAt = req.getActualServiceAt();
        validateActualServiceAt(actualAt, principal);

        Estimate estimate = requireFreshDraftEstimate(req.getEstimateId());

        ServiceTicket ticket = new ServiceTicket();
        if (kind == BackfillKind.SUPPLEMENT) {
            ServiceTicketJpa parent = requireSupplementableParent(req.getParentTicketId());
            ticket.setTicketType(parent.getTicketType());
            ticket.setCustomerId(parent.getCustomerId());
            ticket.setVehicleId(parent.getVehicleId());
            ticket.setBackfillParentTicketId(parent.getServiceTicketId());
            ticket.setTicketCode(nextSupplementCode(parent));
        } else {
            TicketType type = parseEnum(TicketType.class, req.getTicketType(), "Loại phiếu");
            ticket.setTicketType(type);
            if (type == TicketType.PARTS_SALE) {
                CustomerProfile customer = customerRepository.findById(requireId(req.getCustomerId(), "khách hàng"))
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng: " + req.getCustomerId()));
                ticket.setCustomerId(customer.getCustomerId());
                ticket.setTicketCode(ticketCodeGenerator.generateCode(actualAt.toLocalDate(), CodePrefix.PARTS_SALE));
            } else {
                resolveServiceCustomerAndVehicle(req, ticket);
                ticket.setTicketCode(ticketCodeGenerator.generateCode(actualAt.toLocalDate(), CodePrefix.SERVICE_TICKET));
            }
        }

        if (ticket.getTicketType() == TicketType.SERVICE) {
            ticket.setBackfillAdvisorId(validateStaffRole(req.getAdvisorId(), "ADVISOR", "cố vấn"));
            ticket.setBackfillTechnicianId(validateStaffRole(req.getTechnicianId(), "TECHNICIAN", "kỹ thuật viên"));
        }

        ticket.setBookingId(null);
        ticket.setCreatedBy(staffId);
        ticket.setCustomerRequest(trimToNull(req.getNote()));
        ticket.setSafetyInspectionEnabled(false);
        ticket.setReceivedAt(actualAt);
        ticket.initializeDefaults();
        ticket.setTicketStatus(TicketStatus.HOLDING);
        ticket.setEntryMode(EntryMode.BACKFILL);
        ticket.setBackfillKind(kind);
        ticket.setBackfillReason(reason);
        ticket.setBackfillPaymentMethod(paymentMethod);
        ticket.setBackfillReviewStatus(BackfillReviewStatus.PENDING_REVIEW);
        ServiceTicket saved = serviceTicketRepo.save(ticket);
        Integer ticketId = saved.getServiceTicketId();

        estimateInternalApi.linkEstimateToServiceTicket(estimate.getId(), ticketId);
        // Giữ hàng ngay — thiếu tồn thì ném lỗi và rollback cả phiếu
        stockAllocationService.createStockAllocation(estimate.getId(), staffId);

        PaymentProofCreateDto proofDto = new PaymentProofCreateDto();
        proofDto.setItems(proofs);
        paymentProofService.add(ticketId, proofDto, staffId);

        log.info("Backfill ticket {} (id={}, kind={}, type={}) created by staff {} for actual date {}",
                saved.getTicketCode(), ticketId, kind, saved.getTicketType(), staffId, actualAt);
        return toDto(serviceTicketJpaRepo.findByServiceTicketId(ticketId), true);
    }

    // =====================================================================
    // Duyệt / từ chối
    // =====================================================================

    @Transactional
    public BackfillTicketDto approve(Integer serviceTicketId, String note, StaffPrincipal principal) {
        Integer reviewerId = requireReviewer(principal);
        ServiceTicket ticket = requirePendingTicket(serviceTicketId);
        if (Objects.equals(ticket.getCreatedBy(), reviewerId) && !hasRole(principal, ROLE_ADMIN)) {
            throw new IllegalArgumentException("Không được tự duyệt phiếu nhập bù do chính mình nhập — nhờ quản lý khác hoặc admin duyệt.");
        }
        if (paymentProofJpaRepo.countByServiceTicketId(serviceTicketId) < 1) {
            throw new IllegalArgumentException("Phiếu " + ticket.getTicketCode() + " chưa có chứng từ nào — không duyệt được.");
        }
        Estimate estimate = estimateInternalApi.findLatestByServiceTicketId(serviceTicketId);
        if (estimate == null) {
            throw new IllegalArgumentException("Phiếu " + ticket.getTicketCode() + " chưa có báo giá.");
        }

        assignStaffOnApprove(ticket);

        // Đánh dấu duyệt TRƯỚC khi gọi billing: BackfillGuard trong createNewBilling chỉ cho qua phiếu đã APPROVED
        LocalDateTime actualAt = ticket.getReceivedAt();
        ticket.setBackfillReviewStatus(BackfillReviewStatus.APPROVED);
        ticket.setBackfillReviewedBy(reviewerId);
        ticket.setBackfillReviewedAt(LocalDateTime.now());
        ticket.setBackfillReviewNote(trimToNull(note));
        ticket.setTicketStatus(TicketStatus.COMPLETED);
        ticket.setCompletedAt(actualAt);
        serviceTicketRepo.save(ticket);
        // createNewBilling chỉ nhận báo giá ARCHIVED (giống PartsSaleService.createPartsSale)
        estimateService.updateEstimateStatus(estimate.getId(), EstimateEnum.ARCHIVED);

        ServiceBillCreateDto billDto = new ServiceBillCreateDto();
        billDto.setServiceTicketId(serviceTicketId);
        billDto.setEstimateId(estimate.getId());
        billDto.setPaymentStatus("UNPAID");
        ServiceBillDto bill = billingService.createNewBilling(billDto);

        billingService.recordBackfillPayment(bill.getBillId(), ticket.getBackfillPaymentMethod(), actualAt, reviewerId);

        log.info("Backfill ticket {} approved by staff {}", ticket.getTicketCode(), reviewerId);
        return toDto(serviceTicketJpaRepo.findByServiceTicketId(serviceTicketId), true);
    }

    @Transactional
    public BackfillTicketDto reject(Integer serviceTicketId, String note, StaffPrincipal principal) {
        Integer reviewerId = requireReviewer(principal);
        String reviewNote = trimToNull(note);
        if (reviewNote == null) {
            throw new IllegalArgumentException("Vui lòng ghi lý do từ chối để nhân viên biết mà sửa lại.");
        }
        ServiceTicket ticket = requirePendingTicket(serviceTicketId);

        warehouseStockAllocationService.release(serviceTicketId, reviewerId);
        Estimate estimate = estimateInternalApi.findLatestByServiceTicketId(serviceTicketId);
        if (estimate != null && estimate.getStatus() != EstimateEnum.ARCHIVED) {
            estimateService.updateEstimateStatus(estimate.getId(), EstimateEnum.CANCELLED);
        }

        ticket.setTicketStatus(TicketStatus.CANCELLED);
        ticket.setBackfillReviewStatus(BackfillReviewStatus.REJECTED);
        ticket.setBackfillReviewedBy(reviewerId);
        ticket.setBackfillReviewedAt(LocalDateTime.now());
        ticket.setBackfillReviewNote(reviewNote.length() > REASON_MAX_LENGTH
                ? reviewNote.substring(0, REASON_MAX_LENGTH) : reviewNote);
        serviceTicketRepo.save(ticket);

        log.info("Backfill ticket {} rejected by staff {}", ticket.getTicketCode(), reviewerId);
        return toDto(serviceTicketJpaRepo.findByServiceTicketId(serviceTicketId), true);
    }

    // =====================================================================
    // Truy vấn
    // =====================================================================

    @Transactional(readOnly = true)
    public Page<BackfillTicketDto> list(String reviewStatus, Integer createdBy, LocalDate from, LocalDate to,
                                        int page, int size, StaffPrincipal principal) {
        Integer staffId = requireStaffId(principal);
        // Nhân viên thường chỉ xem phiếu mình nhập; quản lý xem tất cả
        Integer creatorFilter = isReviewer(principal) ? createdBy : staffId;
        BackfillReviewStatus status = trimToNull(reviewStatus) == null
                ? null : parseEnum(BackfillReviewStatus.class, reviewStatus, "Trạng thái duyệt");
        int safeSize = Math.min(Math.max(size, 1), 100);
        return serviceTicketJpaRepo.findBackfillTickets(
                        status,
                        creatorFilter,
                        from == null ? null : from.atStartOfDay(),
                        to == null ? null : to.plusDays(1).atStartOfDay(),
                        PageRequest.of(Math.max(page, 0), safeSize))
                .map(t -> toDto(t, false));
    }

    @Transactional(readOnly = true)
    public BackfillTicketDto detail(Integer serviceTicketId, StaffPrincipal principal) {
        Integer staffId = requireStaffId(principal);
        ServiceTicketJpa ticket = serviceTicketJpaRepo.findByServiceTicketId(serviceTicketId);
        if (ticket == null || ticket.getEntryMode() != EntryMode.BACKFILL) {
            throw new IllegalArgumentException("Không tìm thấy phiếu nhập bù: " + serviceTicketId);
        }
        if (!isReviewer(principal) && !Objects.equals(ticket.getCreatedBy(), staffId)) {
            throw new IllegalArgumentException("Bạn chỉ xem được phiếu nhập bù do mình nhập.");
        }
        return toDto(ticket, true);
    }

    @Transactional(readOnly = true)
    public List<BackfillStaffStatDto> stats(LocalDate from, LocalDate to, StaffPrincipal principal) {
        requireReviewer(principal);
        List<ServiceTicketJpa> tickets = serviceTicketJpaRepo.findBackfillTicketsCreatedBetween(
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay());

        Map<Integer, BackfillStaffStatDto> byStaff = new LinkedHashMap<>();
        for (ServiceTicketJpa t : tickets) {
            Integer creator = t.getCreatedBy();
            BackfillStaffStatDto stat = byStaff.computeIfAbsent(creator, id -> {
                BackfillStaffStatDto s = new BackfillStaffStatDto();
                s.setStaffId(id);
                s.setStaffName(staffName(id));
                return s;
            });
            stat.setTotal(stat.getTotal() + 1);
            BackfillReviewStatus status = t.getBackfillReviewStatus();
            if (status == BackfillReviewStatus.APPROVED) {
                stat.setApproved(stat.getApproved() + 1);
                Estimate estimate = estimateInternalApi.findLatestByServiceTicketId(t.getServiceTicketId());
                if (estimate != null && estimate.getTotalPrice() != null) {
                    stat.setApprovedAmount(stat.getApprovedAmount().add(estimate.getTotalPrice()));
                }
            } else if (status == BackfillReviewStatus.REJECTED) {
                stat.setRejected(stat.getRejected() + 1);
            } else {
                stat.setPending(stat.getPending() + 1);
            }
        }
        List<BackfillStaffStatDto> result = new ArrayList<>(byStaff.values());
        result.sort((a, b) -> Long.compare(b.getTotal(), a.getTotal()));
        return result;
    }

    /** Tra phiếu gốc theo mã để nhập thiếu dòng. */
    @Transactional(readOnly = true)
    public BackfillParentDto findParent(String ticketCode, StaffPrincipal principal) {
        requireStaffId(principal);
        String code = trimToNull(ticketCode);
        if (code == null) {
            throw new IllegalArgumentException("Vui lòng nhập mã phiếu gốc.");
        }
        ServiceTicketJpa parent = serviceTicketJpaRepo.findByTicketCode(code.toUpperCase(Locale.ROOT))
                .or(() -> serviceTicketJpaRepo.findByTicketCode(code))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu " + code + "."));
        requireSupplementableParent(parent.getServiceTicketId());

        BackfillParentDto dto = new BackfillParentDto();
        dto.setServiceTicketId(parent.getServiceTicketId());
        dto.setTicketCode(parent.getTicketCode());
        dto.setTicketType(parent.getTicketType() == null ? null : parent.getTicketType().name());
        dto.setTicketStatus(parent.getTicketStatus() == null ? null : parent.getTicketStatus().name());
        dto.setCustomerId(parent.getCustomerId());
        customerRepository.findById(parent.getCustomerId()).ifPresent(c -> {
            dto.setCustomerName(c.getFullName());
            dto.setCustomerPhone(c.getPhone());
        });
        dto.setVehicleId(parent.getVehicleId());
        if (parent.getVehicleId() != null) {
            vehicleRepository.findById(parent.getVehicleId()).ifPresent(v -> dto.setLicensePlate(v.getLicensePlate()));
        }
        dto.setReceivedAt(parent.getReceivedAt());
        dto.setDeliveredAt(parent.getDeliveredAt());
        return dto;
    }

    /** Phiếu chưa huỷ của khách trong ngày — FE cảnh báo trước khi gửi nhập bù. */
    @Transactional(readOnly = true)
    public List<BackfillDuplicateDto> sameDayTickets(Integer customerId, LocalDate date, StaffPrincipal principal) {
        requireStaffId(principal);
        if (customerId == null || date == null) {
            return List.of();
        }
        return findSameDay(customerId, date, null);
    }

    public BackfillPolicyDto policy(StaffPrincipal principal) {
        requireStaffId(principal);
        int maxDays = maxDaysBack(principal);
        return new BackfillPolicyDto(maxDays, earliestAllowed(maxDays), isReviewer(principal));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private void validateActualServiceAt(LocalDateTime actualAt, StaffPrincipal principal) {
        if (actualAt == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày giờ thực tế khách tới.");
        }
        if (actualAt.isAfter(LocalDateTime.now().plusMinutes(FUTURE_TOLERANCE_MINUTES))) {
            throw new IllegalArgumentException("Ngày giờ thực tế không được ở tương lai.");
        }
        int maxDays = maxDaysBack(principal);
        if (actualAt.isBefore(earliestAllowed(maxDays))) {
            throw new IllegalArgumentException("Chỉ được nhập bù phiếu trong " + maxDays + " ngày gần nhất"
                    + (isReviewer(principal) ? "." : " — phiếu cũ hơn cần quản lý nhập."));
        }
    }

    private int maxDaysBack(StaffPrincipal principal) {
        return isReviewer(principal) ? REVIEWER_MAX_DAYS_BACK : STAFF_MAX_DAYS_BACK;
    }

    private LocalDateTime earliestAllowed(int maxDays) {
        return LocalDate.now().minusDays(maxDays).atStartOfDay();
    }

    /** Báo giá phải DRAFT và chưa gắn phiếu nào — tránh dùng lại báo giá của phiếu khác. */
    private Estimate requireFreshDraftEstimate(Integer estimateId) {
        Estimate estimate = estimateInternalApi.findById(requireId(estimateId, "báo giá"));
        if (estimate == null) {
            throw new IllegalArgumentException("Không tìm thấy báo giá: " + estimateId);
        }
        if (estimate.getStatus() != EstimateEnum.DRAFT) {
            throw new IllegalArgumentException("Báo giá không ở trạng thái nháp (hiện tại: " + estimate.getStatus() + ").");
        }
        if (estimate.getServiceTicketId() != null) {
            ServiceTicketJpa linked = serviceTicketJpaRepo.findByServiceTicketId(estimate.getServiceTicketId());
            throw new IllegalArgumentException("Báo giá đã gắn vào phiếu "
                    + (linked != null ? linked.getTicketCode() : estimate.getServiceTicketId())
                    + " — hãy làm mới bảng báo giá rồi nhập lại.");
        }
        return estimate;
    }

    private ServiceTicketJpa requireSupplementableParent(Integer parentTicketId) {
        ServiceTicketJpa parent = serviceTicketJpaRepo.findByServiceTicketId(requireId(parentTicketId, "phiếu gốc"));
        if (parent == null || Boolean.TRUE.equals(parent.getIsDeleted())) {
            throw new IllegalArgumentException("Không tìm thấy phiếu gốc: " + parentTicketId);
        }
        if (parent.getTicketStatus() != TicketStatus.PAID) {
            throw new IllegalArgumentException("Chỉ nhập thiếu cho phiếu đã thanh toán. Phiếu " + parent.getTicketCode()
                    + " đang " + parent.getTicketStatus() + " — sửa trực tiếp báo giá của phiếu đó.");
        }
        return parent;
    }

    /** Mã phiếu con: MÃ_GỐC-NB1, -NB2... — nhìn mã là biết phiếu bổ sung của phiếu nào. */
    private String nextSupplementCode(ServiceTicketJpa parent) {
        long index = serviceTicketJpaRepo.countByBackfillParentTicketId(parent.getServiceTicketId()) + 1;
        String code;
        do {
            code = parent.getTicketCode() + "-NB" + index++;
        } while (serviceTicketJpaRepo.existsByTicketCode(code));
        return code;
    }

    /**
     * Phiếu sửa xe: dùng khách/xe đã chọn, hoặc tra theo SĐT/biển số giống /create-booking,
     * không có thì tạo mới (khách vãng lai lần đầu tới bị miss phiếu vẫn nhập bù được).
     */
    private void resolveServiceCustomerAndVehicle(BackfillCreateRequest req, ServiceTicket ticket) {
        String phone = trimToNull(req.getPhone());
        String plate = trimToNull(req.getLicensePlate());
        plate = plate == null ? null : plate.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");

        CustomerProfile customer = null;
        if (req.getCustomerId() != null) {
            customer = customerRepository.findById(req.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng: " + req.getCustomerId()));
        } else if (phone != null) {
            customer = customerRepository.findByPhone(phone).orElse(null);
        }

        Vehicle vehicle = null;
        if (req.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(req.getVehicleId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe: " + req.getVehicleId()));
        } else if (plate != null) {
            vehicle = vehicleRepository.findByLicensePlate(plate).orElse(null);
        }

        if (customer == null && vehicle != null) {
            customer = vehicle.getCustomer();
        }
        if (customer == null) {
            String fullName = trimToNull(req.getFullName());
            if (fullName == null || (phone == null && plate == null)) {
                throw new IllegalArgumentException("Vui lòng nhập họ tên và số điện thoại hoặc biển số xe của khách.");
            }
            CustomerProfile created = new CustomerProfile();
            created.setPhone(phone);
            created.setFullName(fullName);
            created.setCreatedAt(LocalDateTime.now());
            created.setCustomerType(CustomerType.INDIVIDUAL);
            created.setNotificationChannel(NotificationChannel.ZALO);
            created.setIsDealer(false);
            created.setIsCompany(false);
            customer = customerRepository.save(created);
        }

        if (vehicle != null && vehicle.getCustomer() != null
                && !Objects.equals(vehicle.getCustomer().getCustomerId(), customer.getCustomerId())) {
            throw new IllegalArgumentException("Xe " + vehicle.getLicensePlate() + " đang thuộc khách khác trong hệ thống.");
        }
        if (vehicle == null && plate != null) {
            Vehicle created = new Vehicle();
            created.setLicensePlate(plate);
            created.setCustomer(customer);
            vehicle = vehicleRepository.save(created);
        }

        ticket.setCustomerId(customer.getCustomerId());
        ticket.setVehicleId(vehicle == null ? null : vehicle.getVehicleId());
    }

    private Integer validateStaffRole(Integer staffId, String role, String label) {
        if (staffId == null) {
            return null;
        }
        if (!staffProfileRepo.existsByStaffIdAndRole(staffId, role)) {
            throw new IllegalArgumentException("Nhân viên đã chọn không có vai trò " + label + ".");
        }
        return staffId;
    }

    /**
     * Phân công lúc duyệt chứ không lúc nhập: phân công sớm thì phiếu chờ duyệt hiện
     * vào danh sách việc cần làm của KTV/cố vấn. Phiếu bán hàng không chọn người thì
     * gán người nhập làm cố vấn như luồng bán hàng thường.
     */
    private void assignStaffOnApprove(ServiceTicket ticket) {
        Integer ticketId = ticket.getServiceTicketId();
        Integer advisorId = ticket.getBackfillAdvisorId();
        if (advisorId == null && ticket.getTicketType() == TicketType.PARTS_SALE) {
            try {
                assign(ticketId, ticket.getCreatedBy(), "ADVISOR");
            } catch (Exception ex) {
                log.warn("Cannot assign creator as advisor for backfill ticket {}: {}", ticketId, ex.getMessage());
            }
        } else if (advisorId != null) {
            assign(ticketId, advisorId, "ADVISOR");
        }
        if (ticket.getBackfillTechnicianId() != null) {
            assign(ticketId, ticket.getBackfillTechnicianId(), "TECHNICIAN");
        }
    }

    private void assign(Integer ticketId, Integer staffId, String role) {
        AssignStaffDto dto = new AssignStaffDto();
        dto.setStaffId(staffId);
        dto.setRoleInTicket(role);
        dto.setNote("Phân công khi duyệt phiếu nhập bù");
        ticketAssignmentService.assignStaff(ticketId, dto);
    }

    private ServiceTicket requirePendingTicket(Integer serviceTicketId) {
        ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(requireId(serviceTicketId, "phiếu"));
        if (ticket == null || ticket.getEntryMode() != EntryMode.BACKFILL) {
            throw new IllegalArgumentException("Không tìm thấy phiếu nhập bù: " + serviceTicketId);
        }
        if (ticket.getBackfillReviewStatus() != BackfillReviewStatus.PENDING_REVIEW
                || ticket.getTicketStatus() != TicketStatus.HOLDING) {
            throw new IllegalArgumentException("Phiếu " + ticket.getTicketCode() + " không còn chờ duyệt (hiện tại: "
                    + ticket.getBackfillReviewStatus() + ").");
        }
        return ticket;
    }

    private BackfillTicketDto toDto(ServiceTicketJpa t, boolean withSameDay) {
        BackfillTicketDto dto = new BackfillTicketDto();
        dto.setServiceTicketId(t.getServiceTicketId());
        dto.setTicketCode(t.getTicketCode());
        dto.setTicketType(t.getTicketType() == null ? null : t.getTicketType().name());
        dto.setTicketStatus(t.getTicketStatus() == null ? null : t.getTicketStatus().name());
        dto.setKind(t.getBackfillKind() == null ? null : t.getBackfillKind().name());
        dto.setReviewStatus(t.getBackfillReviewStatus() == null ? null : t.getBackfillReviewStatus().name());

        dto.setParentTicketId(t.getBackfillParentTicketId());
        if (t.getBackfillParentTicketId() != null) {
            ServiceTicketJpa parent = serviceTicketJpaRepo.findByServiceTicketId(t.getBackfillParentTicketId());
            dto.setParentTicketCode(parent == null ? null : parent.getTicketCode());
        }

        dto.setCustomerId(t.getCustomerId());
        if (t.getCustomerId() != null) {
            customerRepository.findById(t.getCustomerId()).ifPresent(c -> {
                dto.setCustomerName(c.getFullName());
                dto.setCustomerPhone(c.getPhone());
            });
        }
        dto.setVehicleId(t.getVehicleId());
        if (t.getVehicleId() != null) {
            vehicleRepository.findById(t.getVehicleId()).ifPresent(v -> dto.setLicensePlate(v.getLicensePlate()));
        }

        dto.setActualServiceAt(t.getReceivedAt());
        dto.setCreatedAt(t.getCreatedAt());
        if (t.getReceivedAt() != null && t.getCreatedAt() != null) {
            dto.setDaysLate(ChronoUnit.DAYS.between(t.getReceivedAt().toLocalDate(), t.getCreatedAt().toLocalDate()));
        }
        dto.setCreatedBy(t.getCreatedBy());
        dto.setCreatedByName(staffName(t.getCreatedBy()));

        dto.setReason(t.getBackfillReason());
        dto.setNote(t.getCustomerRequest());
        dto.setPaymentMethod(t.getBackfillPaymentMethod());
        dto.setAdvisorId(t.getBackfillAdvisorId());
        dto.setAdvisorName(staffName(t.getBackfillAdvisorId()));
        dto.setTechnicianId(t.getBackfillTechnicianId());
        dto.setTechnicianName(staffName(t.getBackfillTechnicianId()));

        Estimate estimate = estimateInternalApi.findLatestByServiceTicketId(t.getServiceTicketId());
        if (estimate != null) {
            dto.setEstimateId(estimate.getId());
            dto.setTotalAmount(estimate.getTotalPrice() == null ? BigDecimal.ZERO : estimate.getTotalPrice());
        }
        dto.setProofCount(paymentProofJpaRepo.countByServiceTicketId(t.getServiceTicketId()));

        dto.setReviewedBy(t.getBackfillReviewedBy());
        dto.setReviewedByName(staffName(t.getBackfillReviewedBy()));
        dto.setReviewedAt(t.getBackfillReviewedAt());
        dto.setReviewNote(t.getBackfillReviewNote());

        if (withSameDay && t.getCustomerId() != null && t.getReceivedAt() != null) {
            dto.setSameDayTickets(findSameDay(t.getCustomerId(), t.getReceivedAt().toLocalDate(), t.getServiceTicketId()));
        }
        return dto;
    }

    private List<BackfillDuplicateDto> findSameDay(Integer customerId, LocalDate date, Integer excludeTicketId) {
        return serviceTicketJpaRepo.findActiveByCustomerReceivedBetween(
                        customerId, date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                .stream()
                .filter(t -> !Objects.equals(t.getServiceTicketId(), excludeTicketId))
                .map(t -> new BackfillDuplicateDto(
                        t.getServiceTicketId(),
                        t.getTicketCode(),
                        t.getTicketType() == null ? null : t.getTicketType().name(),
                        t.getTicketStatus() == null ? null : t.getTicketStatus().name(),
                        t.getEntryMode() == null ? EntryMode.NORMAL.name() : t.getEntryMode().name(),
                        t.getReceivedAt()))
                .toList();
    }

    private String staffName(Integer staffId) {
        if (staffId == null) {
            return null;
        }
        return staffProfileRepo.findById(staffId).map(StaffProfileJpa::getFullName).orElse(null);
    }

    private Integer requireStaffId(StaffPrincipal principal) {
        Integer staffId = principal == null ? null : principal.getStaffId();
        if (staffId == null) {
            throw new IllegalArgumentException("Không xác định được nhân viên đang đăng nhập.");
        }
        return staffId;
    }

    private Integer requireReviewer(StaffPrincipal principal) {
        Integer staffId = requireStaffId(principal);
        if (!isReviewer(principal)) {
            throw new IllegalArgumentException("Chỉ quản lý hoặc admin được duyệt phiếu nhập bù.");
        }
        return staffId;
    }

    private boolean isReviewer(StaffPrincipal principal) {
        return hasRole(principal, ROLE_MANAGER) || hasRole(principal, ROLE_ADMIN);
    }

    private boolean hasRole(StaffPrincipal principal, String role) {
        if (principal == null || principal.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : principal.getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static Integer requireId(Integer id, String label) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Thiếu " + label + ".");
        }
        return id;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String label) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(label + " không hợp lệ: " + raw);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
