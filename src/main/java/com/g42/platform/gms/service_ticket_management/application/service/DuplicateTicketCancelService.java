package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.authz.application.PermissionResolver;
import com.g42.platform.gms.billing.app.service.BillingService;
import com.g42.platform.gms.common.enums.EstimateEnum;
import com.g42.platform.gms.dashboard.application.service.DashboardSummaryService;
import com.g42.platform.gms.estimation.api.internal.EstimateInternalApi;
import com.g42.platform.gms.estimation.app.service.EstimateService;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.DuplicateCancelSummary;
import com.g42.platform.gms.service_ticket_management.domain.entity.BackfillGuard;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.exception.CheckInException;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import com.g42.platform.gms.vehicle.repository.VehicleRepository;
import com.g42.platform.gms.warehouse.app.service.allocation.TicketStockReversalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Huỷ phiếu dịch vụ bị tạo trùng — kể cả phiếu đã hoàn thành (có hoá đơn) hoặc đã thanh toán.
 *
 * <p>Nút "Hủy phiếu" thường chỉ mở tới REPAIRING và chặn khi phiếu còn giữ / đã xuất hàng; phiếu
 * đã có hoá đơn thì không có đường nào để huỷ. Ở đây làm trọn một lượt, trong MỘT transaction
 * (lỗi ở bước nào cũng quay lui hết):
 * <ol>
 *   <li>Kho: nhả giữ hàng, huỷ phiếu xuất nháp, cộng lại hàng đã xuất ({@link TicketStockReversalService}).</li>
 *   <li>Hoá đơn + giao dịch thu tiền → VOID (không xoá, còn truy vết).</li>
 *   <li>Báo giá chưa chốt → CANCELLED (báo giá đã chốt hoá đơn giữ nguyên làm chứng từ).</li>
 *   <li>Phiếu → CANCELLED, ghi lý do, người huỷ, thời điểm và phiếu gốc mà nó trùng.</li>
 * </ol>
 * Sau khi commit thì tính lại các ngày bị ảnh hưởng trên dashboard (bảng cache theo ngày chỉ tự
 * tính lại 62 ngày gần nhất vào ban đêm).
 *
 * <p>Chỉ ADMIN: endpoint chặn bằng mã quyền SERVICE_TICKET_CANCEL_DUPLICATE (khoá cứng cho ADMIN
 * qua PermissionCodes.ROLE_RESTRICTED), và ở đây kiểm tra lại vai trò — thao tác này xoá doanh
 * thu đã ghi nhận, không để một cú tick nhầm ở màn phân quyền mở nó cho vai trò khác.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateTicketCancelService {

    static final int REASON_MAX_LENGTH = 500;

    private final ServiceTicketRepository serviceTicketRepository;
    private final CustomerProfileRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final BillingService billingService;
    private final TicketStockReversalService ticketStockReversalService;
    private final EstimateInternalApi estimateInternalApi;
    private final EstimateService estimateService;
    private final DashboardSummaryService dashboardSummaryService;
    private final PlatformTransactionManager transactionManager;

    /** Xem trước những gì sẽ bị huỷ / hoàn tác. Không ghi gì. */
    @Transactional(readOnly = true)
    public DuplicateCancelSummary preview(Integer serviceTicketId, String duplicateOfTicketCode, StaffPrincipal principal) {
        requireAdmin(principal);
        ServiceTicketJpa ticket = requireCancellable(serviceTicketId);
        ServiceTicketJpa original = isBlank(duplicateOfTicketCode) ? null : requireOriginal(ticket, duplicateOfTicketCode);

        List<BillingService.VoidedBill> bills = billingService.activeBillsOfTicket(serviceTicketId);
        TicketStockReversalService.Result stock = ticketStockReversalService.preview(serviceTicketId);
        return buildSummary(false, ticket, original, bills, stock);
    }

    @Transactional
    public DuplicateCancelSummary cancel(Integer serviceTicketId, String duplicateOfTicketCode, String reason,
                                         StaffPrincipal principal) {
        Integer staffId = requireAdmin(principal);
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.isEmpty()) {
            throw CheckInException.field("reason", "Vui lòng ghi lý do huỷ phiếu trùng.");
        }
        if (cleanReason.length() > REASON_MAX_LENGTH) {
            throw CheckInException.field("reason", "Lý do tối đa " + REASON_MAX_LENGTH + " ký tự.");
        }
        if (isBlank(duplicateOfTicketCode)) {
            throw CheckInException.field("duplicateOfTicketCode", "Vui lòng nhập mã phiếu gốc (phiếu được giữ lại).");
        }

        ServiceTicketJpa ticket = requireCancellable(serviceTicketId);
        ServiceTicketJpa original = requireOriginal(ticket, duplicateOfTicketCode);
        TicketStatus previousStatus = ticket.getTicketStatus();

        // Ngày cần tính lại dashboard — lấy TRƯỚC khi đổi dữ liệu
        Set<LocalDate> affectedDates = new TreeSet<>();
        addDate(affectedDates, ticket.getReceivedAt());
        addDate(affectedDates, ticket.getCompletedAt());
        addDate(affectedDates, ticket.getDeliveredAt());

        String note = "phiếu " + ticket.getTicketCode() + " trùng " + original.getTicketCode() + " — " + cleanReason;

        // 1) Kho
        TicketStockReversalService.Result stock = ticketStockReversalService.reverse(serviceTicketId, staffId, note);

        // 2) Hoá đơn + giao dịch thu tiền
        List<BillingService.VoidedBill> bills = billingService.voidBillsOfTicket(serviceTicketId);
        ZoneId zone = ZoneId.systemDefault();
        for (BillingService.VoidedBill bill : bills) {
            if (bill.paidAt() != null) {
                affectedDates.add(LocalDateTime.ofInstant(bill.paidAt(), zone).toLocalDate());
            }
        }

        // 3) Báo giá chưa chốt
        Estimate estimate = estimateInternalApi.findLatestByServiceTicketId(serviceTicketId);
        if (estimate != null && estimate.getStatus() != EstimateEnum.ARCHIVED
                && estimate.getStatus() != EstimateEnum.CANCELLED) {
            estimateService.updateEstimateStatus(estimate.getId(), EstimateEnum.CANCELLED);
        }

        // 4) Phiếu — đọc lại vì các bước trên có thể đã lưu phiếu qua repo domain
        ServiceTicketJpa fresh = serviceTicketRepository.findByServiceTicketId(serviceTicketId);
        LocalDateTime now = LocalDateTime.now();
        fresh.setTicketStatus(TicketStatus.CANCELLED);
        fresh.setCancelReason(cleanReason);
        fresh.setCancelledAt(now);
        fresh.setCancelledBy(staffId);
        fresh.setDuplicateOfTicketId(original.getServiceTicketId());
        fresh.setUpdatedAt(now);
        serviceTicketRepository.save(fresh);

        log.warn("ADMIN staff {} huỷ phiếu trùng {} (trạng thái cũ {}), giữ phiếu gốc {}: {} hoá đơn VOID, {} dòng kho hoàn tác. Lý do: {}",
                staffId, ticket.getTicketCode(), previousStatus, original.getTicketCode(),
                bills.size(), stock.lines().size(), cleanReason);

        refreshDashboardAfterCommit(affectedDates);
        return buildSummary(true, fresh, original, bills, stock);
    }

    // ------------------------------------------------------------------------------------------

    private Integer requireAdmin(StaffPrincipal principal) {
        if (principal == null || principal.getStaffId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        }
        List<String> roles = principal.getRoleCodes();
        boolean admin = roles != null && roles.stream()
                .map(r -> r == null ? "" : r.trim().toUpperCase())
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .anyMatch(PermissionResolver.SUPER_ROLE::equals);
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Admin mới được huỷ phiếu trùng.");
        }
        return principal.getStaffId();
    }

    private ServiceTicketJpa requireCancellable(Integer serviceTicketId) {
        ServiceTicketJpa ticket = serviceTicketId == null ? null : serviceTicketRepository.findByServiceTicketId(serviceTicketId);
        if (ticket == null || Boolean.TRUE.equals(ticket.getIsDeleted())) {
            throw new CheckInException("Không tìm thấy phiếu dịch vụ #" + serviceTicketId + ".");
        }
        if (ticket.getTicketStatus() == TicketStatus.CANCELLED) {
            throw new CheckInException("Phiếu " + ticket.getTicketCode() + " đã bị huỷ rồi.");
        }
        // Phiếu nhập bù chưa duyệt đã có đường riêng: "Từ chối" ở /ticket-backfill (nhả hàng luôn)
        BackfillGuard.requireNotUnapprovedBackfill(ticket.getEntryMode(), ticket.getBackfillReviewStatus(),
                ticket.getTicketCode());
        return ticket;
    }

    private ServiceTicketJpa requireOriginal(ServiceTicketJpa ticket, String code) {
        String clean = code.trim();
        ServiceTicketJpa original = serviceTicketRepository.findByTicketCode(clean).orElse(null);
        if (original == null || Boolean.TRUE.equals(original.getIsDeleted())) {
            throw CheckInException.field("duplicateOfTicketCode", "Không tìm thấy phiếu gốc " + clean + ".");
        }
        if (Objects.equals(original.getServiceTicketId(), ticket.getServiceTicketId())) {
            throw CheckInException.field("duplicateOfTicketCode", "Phiếu gốc phải là một phiếu KHÁC phiếu đang huỷ.");
        }
        if (original.getTicketStatus() == TicketStatus.CANCELLED) {
            throw CheckInException.field("duplicateOfTicketCode",
                    "Phiếu gốc " + clean + " đã bị huỷ — chọn phiếu còn hiệu lực để giữ lại.");
        }
        return original;
    }

    private DuplicateCancelSummary buildSummary(boolean applied, ServiceTicketJpa ticket, ServiceTicketJpa original,
                                                List<BillingService.VoidedBill> bills,
                                                TicketStockReversalService.Result stock) {
        BigDecimal paidVoided = bills.stream()
                .filter(b -> "PAID".equals(b.previousStatus()))
                .map(b -> b.finalAmount() == null ? BigDecimal.ZERO : b.finalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> warnings = new ArrayList<>();
        if (paidVoided.signum() > 0) {
            warnings.add("Doanh thu đã ghi nhận của phiếu này sẽ bị loại khỏi báo cáo doanh thu, báo cáo khách hàng và dashboard.");
        }
        // Hai phiếu trùng có thể là "trùng ở hệ thống, nhưng tiền thật thu ở phiếu này": huỷ đi mà
        // phiếu gốc chưa thu thì garage mất dấu khoản tiền đã nhận.
        if (original != null && paidVoided.signum() > 0 && original.getTicketStatus() != TicketStatus.PAID) {
            warnings.add("Phiếu gốc " + original.getTicketCode() + " CHƯA thanh toán. Nếu tiền thật đã thu ở phiếu này, "
                    + "hãy thu lại ở phiếu gốc sau khi huỷ, nếu không khoản tiền đó sẽ không còn trên hệ thống.");
        }
        if (ticket.getTicketStatus() == TicketStatus.PAID) {
            warnings.add("Tin nhắn mời đánh giá đã gửi cho khách khi thanh toán phiếu này — không thu hồi được.");
        }
        if (original != null && !Objects.equals(original.getCustomerId(), ticket.getCustomerId())
                && (original.getVehicleId() == null || !Objects.equals(original.getVehicleId(), ticket.getVehicleId()))) {
            warnings.add("Phiếu gốc khác cả khách hàng lẫn xe — kiểm tra kỹ hai phiếu có đúng là trùng nhau không.");
        }
        if (!stock.lines().isEmpty()) {
            warnings.add("Hàng của phiếu này sẽ được cộng lại vào kho. Nếu thực tế hàng đã ra khỏi kho hai lần "
                    + "(không phải trùng), đừng huỷ theo cách này.");
        }

        return DuplicateCancelSummary.builder()
                .applied(applied)
                .ticket(brief(ticket))
                .original(original == null ? null : brief(original))
                .bills(bills.stream().map(b -> DuplicateCancelSummary.BillLine.builder()
                        .billId(b.billId())
                        .finalAmount(b.finalAmount())
                        .paymentStatus(b.previousStatus())
                        .paidAt(b.paidAt())
                        .transactionCount(b.voidedTransactionCount())
                        .build()).toList())
                .paidAmountVoided(paidVoided)
                .stockLines(stock.lines().stream().map(l -> DuplicateCancelSummary.StockLine.builder()
                        .action(l.action().name())
                        .warehouseId(l.warehouseId())
                        .itemId(l.itemId())
                        .itemName(l.itemName())
                        .sku(l.sku())
                        .quantity(l.quantity())
                        .issueCode(l.issueCode())
                        .build()).toList())
                .draftIssuesCancelled(stock.draftIssuesCancelled())
                .confirmedIssuesReversed(stock.confirmedIssuesReversed())
                .openReturnsCancelled(stock.openReturnsCancelled())
                .warnings(warnings)
                .build();
    }

    private DuplicateCancelSummary.TicketBrief brief(ServiceTicketJpa t) {
        String name = null;
        String phone = null;
        if (Boolean.TRUE.equals(t.getIsWalkIn())) {
            name = t.getWalkInName();
            phone = t.getWalkInPhone();
        } else if (t.getCustomerId() != null) {
            CustomerProfile c = customerRepository.findById(t.getCustomerId()).orElse(null);
            if (c != null) {
                name = c.getFullName();
                phone = c.getPhone();
            }
        }
        String plate = null;
        if (t.getVehicleId() != null) {
            plate = vehicleRepository.findById(t.getVehicleId()).map(Vehicle::getLicensePlate).orElse(null);
        }
        return DuplicateCancelSummary.TicketBrief.builder()
                .serviceTicketId(t.getServiceTicketId())
                .ticketCode(t.getTicketCode())
                .ticketStatus(t.getTicketStatus())
                .ticketType(t.getTicketType())
                .receivedAt(t.getReceivedAt())
                .customerName(name)
                .customerPhone(phone)
                .licensePlate(plate)
                .customerId(t.getCustomerId())
                .vehicleId(t.getVehicleId())
                .build();
    }

    /**
     * Tính lại dashboard SAU khi commit — chạy trước commit thì bộ tính không thấy dữ liệu mới.
     * Trong afterCommit kết nối của transaction cũ vẫn còn gắn với thread và không còn commit nào
     * phía sau, nên phải mở transaction MỚI (REQUIRES_NEW), không thì phần ghi bảng cache bị bỏ.
     */
    private void refreshDashboardAfterCommit(Set<LocalDate> dates) {
        if (dates.isEmpty()) {
            return;
        }
        TransactionTemplate newTx = new TransactionTemplate(transactionManager);
        newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Runnable refresh = () -> {
            for (LocalDate d : dates) {
                try {
                    newTx.executeWithoutResult(status -> dashboardSummaryService.refresh(d, d));
                } catch (RuntimeException ex) {
                    log.error("Huỷ phiếu trùng: không tính lại được dashboard ngày {} — bấm \"Tính lại\" ở dashboard", d, ex);
                }
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    refresh.run();
                }
            });
        } else {
            refresh.run();
        }
    }

    private static void addDate(Set<LocalDate> dates, LocalDateTime value) {
        if (value != null) {
            dates.add(value.toLocalDate());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
