package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.authz.application.PermissionResolver;
import com.g42.platform.gms.billing.app.service.BillingService;
import com.g42.platform.gms.customer.application.service.WalkInCustomerService;
import com.g42.platform.gms.dashboard.application.service.DashboardSummaryService;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ReassignCandidate;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ReassignCustomerPreview;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ReassignCustomerRequest;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.exception.CheckInException;
import com.g42.platform.gms.vehicle.support.PlateKeys;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import com.g42.platform.gms.vehicle.repository.VehicleRepository;
import com.g42.platform.gms.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Chuyển phiếu dịch vụ bị gán nhầm sang đúng khách hàng / xe.
 *
 * <p>Tình huống: lễ tân chọn nhầm một khách CÓ SẴN (A) cho lượt xe của khách B, nhập biển số của
 * B vào hồ sơ A rồi tạo phiếu. Các ô sửa đầu phiếu không cứu được vì chúng sửa THẲNG hồ sơ A và
 * dòng xe đang gắn phiếu; huỷ rồi tạo lại thì mất luồng phiếu và để lại xe rác trong hồ sơ A.
 *
 * <p>Ở đây, trong MỘT transaction:
 * <ol>
 *   <li>Xác định xe đích: xe có sẵn của B, hoặc tạo xe mới cho B, hoặc — khi xe cũ không còn lịch
 *       sử nào khác và người dùng chọn bỏ nó khỏi hồ sơ A — chuyển hẳn dòng xe cũ sang B.</li>
 *   <li>Đổi customer_id / vehicle_id của phiếu và mọi dữ liệu sinh ra TỪ phiếu: lịch hẹn gốc
 *       (nếu không phiếu nào khác dùng chung), số km ghi lúc tiếp nhận, nhắc lịch bảo dưỡng, bảo
 *       hành phụ tùng, lượt dùng khuyến mãi. Hoá đơn / giao dịch / kho không lưu khách nên không
 *       phải đụng.</li>
 *   <li>Nếu chọn, xoá xe cũ khỏi hồ sơ A (chỉ khi xe đó không còn lịch sử nào khác).</li>
 *   <li>Ghi nhật ký service_ticket_customer_change.</li>
 * </ol>
 * Hồ sơ A và B KHÔNG bị sửa (tên, SĐT giữ nguyên) — đây là khác biệt cốt lõi với các ô sửa đầu phiếu.
 *
 * <p>Quyền: endpoint chặn bằng SERVICE_TICKET_REASSIGN_CUSTOMER; phiếu đã thu tiền thì kiểm tra
 * thêm VAI TRÒ ADMIN / MANAGER ở đây (yêu cầu nghiệp vụ, không để một cú tick quyền mở ra).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCustomerReassignService {

    static final int REASON_MAX_LENGTH = 500;
    static final int PLATE_MAX_LENGTH = 20;
    static final int MODEL_MAX_LENGTH = 255;
    private static final Set<String> PAID_REASSIGN_ROLES = Set.of(PermissionResolver.SUPER_ROLE, "MANAGER");
    private static final int CANDIDATE_LIMIT = 20;

    private final JdbcTemplate jdbc;
    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;
    private final CustomerProfileRepository customerRepository;
    private final BillingService billingService;
    private final DashboardSummaryService dashboardSummaryService;
    private final PlatformTransactionManager transactionManager;

    // ============================================================================================
    // Tìm khách đích
    // ============================================================================================

    /** Tìm khách theo tên / tên công ty / mã / SĐT (cả SĐT phụ) / email / biển số. Bỏ hồ sơ đã xoá và "Khách lẻ". */
    @Transactional(readOnly = true)
    public List<ReassignCandidate> searchCandidates(String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            return List.of();
        }
        String like = "%" + escapeLike(q) + "%";
        String plateKey = PlateKeys.normalize(q);
        String plateLike = plateKey == null ? "\u0000" : "%" + escapeLike(plateKey) + "%";
        List<ReassignCandidate> customers = jdbc.query(
                "SELECT cp.customer_id, cp.customer_code, cp.full_name, cp.company_name, cp.phone, cp.email"
                        + " FROM customer_profile cp"
                        + " LEFT JOIN customer_auth a ON a.customer_id = cp.customer_id"
                        + " WHERE (a.customer_id IS NULL OR a.status IS NULL OR a.status <> 'DELETED')"
                        + "   AND (cp.customer_code IS NULL OR UPPER(cp.customer_code) <> ?)"
                        + "   AND (cp.full_name LIKE ? OR cp.company_name LIKE ? OR cp.customer_code LIKE ?"
                        + "        OR cp.phone LIKE ? OR cp.email LIKE ?"
                        + "        OR EXISTS (SELECT 1 FROM customer_phone p WHERE p.customer_id = cp.customer_id AND p.phone LIKE ?)"
                        + "        OR EXISTS (SELECT 1 FROM vehicle v WHERE v.customer_id = cp.customer_id AND v.plate_key LIKE ?))"
                        + " ORDER BY cp.customer_id DESC LIMIT " + CANDIDATE_LIMIT,
                (rs, i) -> ReassignCandidate.builder()
                        .customerId(rs.getInt("customer_id"))
                        .customerCode(rs.getString("customer_code"))
                        .fullName(rs.getString("full_name"))
                        .companyName(rs.getString("company_name"))
                        .phone(rs.getString("phone"))
                        .email(rs.getString("email"))
                        .vehicles(new ArrayList<>())
                        .build(),
                WalkInCustomerService.WALK_IN_CUSTOMER_CODE, like, like, like, like, like, like, plateLike);
        if (customers.isEmpty()) {
            return customers;
        }
        Map<Integer, ReassignCandidate> byId = new LinkedHashMap<>();
        customers.forEach(c -> byId.put(c.getCustomerId(), c));
        String ids = String.join(",", byId.keySet().stream().map(String::valueOf).toList());
        jdbc.query("SELECT vehicle_id, customer_id, license_plate, brand, model FROM vehicle"
                        + " WHERE customer_id IN (" + ids + ") ORDER BY vehicle_id",
                rs -> {
                    ReassignCandidate c = byId.get(rs.getInt("customer_id"));
                    if (c != null) {
                        c.getVehicles().add(ReassignCandidate.VehicleLine.builder()
                                .vehicleId(rs.getInt("vehicle_id"))
                                .licensePlate(rs.getString("license_plate"))
                                .brand(rs.getString("brand"))
                                .model(rs.getString("model"))
                                .build());
                    }
                });
        return customers;
    }

    // ============================================================================================
    // Xem trước
    // ============================================================================================

    @Transactional(readOnly = true)
    public ReassignCustomerPreview preview(Integer serviceTicketId, ReassignCustomerRequest request, StaffPrincipal principal) {
        requireStaff(principal);
        TicketRow ticket = requireTicket(serviceTicketId, false);
        boolean paid = isPaid(ticket);
        boolean allowed = !paid || hasPaidReassignRole(principal);

        ReassignCustomerPreview.ReassignCustomerPreviewBuilder out = ReassignCustomerPreview.builder()
                .applied(false)
                .serviceTicketId(ticket.id)
                .ticketCode(ticket.code)
                .ticketStatus(ticket.status)
                .paid(paid)
                .allowed(allowed)
                .blockedReason(allowed ? null : paidBlockedMessage())
                .from(party(ticket.customerId, ticket.vehicleId, ticket).dto())
                .history(history(ticket.id));

        Integer targetId = request == null ? null : request.getTargetCustomerId();
        if (targetId == null) {
            out.oldVehicle(oldVehicleInfo(ticket, null, false, null));
            out.moved(List.of());
            out.warnings(baseWarnings(ticket, paid));
            return out.build();
        }

        Plan plan = plan(ticket, request);
        Party to = plan.toParty();
        out.to(to.dto())
                .vehicleMode(plan.vehicleMode)
                .oldVehicle(plan.oldVehicle)
                .moved(describeMoves(ticket, plan))
                .warnings(warnings(ticket, paid, plan));
        return out.build();
    }

    // ============================================================================================
    // Chuyển thật
    // ============================================================================================

    @Transactional
    public ReassignCustomerPreview reassign(Integer serviceTicketId, ReassignCustomerRequest request, StaffPrincipal principal) {
        Integer staffId = requireStaff(principal);
        if (request == null || request.getTargetCustomerId() == null) {
            throw CheckInException.field("targetCustomerId", "Vui lòng chọn khách hàng đúng của phiếu.");
        }
        String reason = request.getReason() == null ? "" : request.getReason().trim();
        if (reason.isEmpty()) {
            throw CheckInException.field("reason", "Vui lòng ghi lý do chuyển phiếu.");
        }
        if (reason.length() > REASON_MAX_LENGTH) {
            throw CheckInException.field("reason", "Lý do tối đa " + REASON_MAX_LENGTH + " ký tự.");
        }

        TicketRow ticket = requireTicket(serviceTicketId, true);
        boolean paid = isPaid(ticket);
        if (paid && !hasPaidReassignRole(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, paidBlockedMessage());
        }

        Plan plan = plan(ticket, request);
        String fromLabel = label(party(ticket.customerId, ticket.vehicleId, ticket));
        List<String> movedLines = describeMoves(ticket, plan);

        // Ngày cần tính lại dashboard
        Set<LocalDate> affectedDates = new TreeSet<>();
        addDate(affectedDates, ticket.receivedAt);
        addDate(affectedDates, ticket.completedAt);
        addDate(affectedDates, ticket.deliveredAt);
        ZoneId zone = ZoneId.systemDefault();
        for (BillingService.VoidedBill bill : billingService.activeBillsOfTicket(ticket.id)) {
            Instant at = bill.paidAt();
            if (at != null) affectedDates.add(LocalDateTime.ofInstant(at, zone).toLocalDate());
        }

        // 1) Xe đích
        Integer newVehicleId = switch (plan.vehicleMode) {
            case "EXISTING" -> plan.targetVehicleId;
            case "TRANSFER" -> transferOldVehicle(ticket.vehicleId, plan);
            case "NEW" -> createVehicle(plan);
            default -> null;
        };

        // 2) Phiếu + dữ liệu đi theo phiếu
        jdbc.update("UPDATE service_ticket SET customer_id = ?, vehicle_id = ?, is_walk_in = 0,"
                        + " walk_in_name = NULL, walk_in_phone = NULL, walk_in_address = NULL, updated_at = NOW()"
                        + " WHERE service_ticket_id = ?",
                plan.target.getCustomerId(), newVehicleId, ticket.id);
        for (Move move : moves(ticket, plan.bookingMovable)) {
            if (!tableExists(move.table)) continue;
            int n = jdbc.update(move.updateSql, move.updateArgs(plan.target.getCustomerId(), newVehicleId));
            if (n > 0) log.info("Chuyển phiếu {}: {} dòng {}", ticket.code, n, move.table);
        }

        // 3) Xe cũ
        String oldVehicleAction = ticket.vehicleId == null ? "NONE" : "KEPT";
        if ("TRANSFER".equals(plan.vehicleMode)) {
            oldVehicleAction = "TRANSFERRED";
        } else if (plan.removeOldVehicle && ticket.vehicleId != null && !Objects.equals(ticket.vehicleId, newVehicleId)) {
            deleteOldVehicle(ticket.vehicleId, newVehicleId);
            oldVehicleAction = "DELETED";
        }

        // 4) Nhật ký
        Party toParty = party(plan.target.getCustomerId(), newVehicleId, null);
        String toLabel = label(toParty);
        jdbc.update("INSERT INTO service_ticket_customer_change (service_ticket_id, from_customer_id, to_customer_id,"
                        + " from_vehicle_id, to_vehicle_id, from_label, to_label, old_vehicle_action, was_paid, reason,"
                        + " changed_by, changed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                ticket.id, ticket.customerId, plan.target.getCustomerId(), ticket.vehicleId, newVehicleId,
                truncate(fromLabel, 500), truncate(toLabel, 500), oldVehicleAction, paid, reason, staffId);

        log.warn("Staff {} chuyển phiếu {} ({}) từ [{}] sang [{}], xe cũ: {}. Lý do: {}",
                staffId, ticket.code, ticket.status, fromLabel, toLabel, oldVehicleAction, reason);

        refreshDashboardAfterCommit(affectedDates);

        ReassignCustomerPreview.OldVehicle oldVehicle = plan.oldVehicle;
        if (oldVehicle != null) oldVehicle.setAction(oldVehicleAction);
        return ReassignCustomerPreview.builder()
                .applied(true)
                .serviceTicketId(ticket.id)
                .ticketCode(ticket.code)
                .ticketStatus(ticket.status)
                .paid(paid)
                .allowed(true)
                .from(party(ticket.customerId, ticket.vehicleId, ticket).dto())
                .to(toParty.dto())
                .vehicleMode(plan.vehicleMode)
                .oldVehicle(oldVehicle)
                .moved(movedLines)
                .warnings(List.of())
                .history(history(ticket.id))
                .build();
    }

    // ============================================================================================
    // Lập kế hoạch (dùng chung cho xem trước và chuyển thật)
    // ============================================================================================

    private Plan plan(TicketRow ticket, ReassignCustomerRequest request) {
        CustomerRow target = requireTargetCustomer(request.getTargetCustomerId());
        Plan plan = new Plan();
        plan.target = target;
        plan.bookingMovable = isBookingMovable(ticket);
        plan.requestedModel = blankToNull(request.getVehicleModel());
        if (plan.requestedModel != null && plan.requestedModel.length() > MODEL_MAX_LENGTH) {
            throw CheckInException.field("vehicleModel", "Loại & kiểu xe tối đa " + MODEL_MAX_LENGTH + " ký tự.");
        }

        Vehicle oldVehicle = ticket.vehicleId == null ? null : vehicleRepository.findById(ticket.vehicleId).orElse(null);
        String plate = blankToNull(request.getLicensePlate());

        if (request.getTargetVehicleId() != null) {
            Vehicle v = vehicleRepository.findById(request.getTargetVehicleId())
                    .orElseThrow(() -> CheckInException.field("targetVehicleId", "Không tìm thấy xe đã chọn."));
            Integer owner = v.getCustomer() == null ? null : v.getCustomer().getCustomerId();
            if (!Objects.equals(owner, target.id)) {
                throw CheckInException.field("targetVehicleId", "Xe đã chọn không thuộc khách hàng " + target.displayName() + ".");
            }
            plan.vehicleMode = "EXISTING";
            plan.targetVehicleId = v.getVehicleId();
            plan.targetPlate = v.getLicensePlate();
            plan.targetModel = v.getModel();
        } else if (plate != null) {
            if (plate.length() > PLATE_MAX_LENGTH) {
                throw CheckInException.field("licensePlate", "Biển số tối đa " + PLATE_MAX_LENGTH + " ký tự.");
            }
            if (PlateKeys.normalize(plate) == null) {
                throw CheckInException.field("licensePlate", "Biển số không hợp lệ.");
            }
            Vehicle existing = vehicleRepository.findByPlateForCustomer(plate, target.id).orElse(null);
            if (existing != null) {
                plan.vehicleMode = "EXISTING";
                plan.targetVehicleId = existing.getVehicleId();
                plan.targetPlate = existing.getLicensePlate();
                plan.targetModel = existing.getModel();
            } else {
                plan.vehicleMode = "NEW";
                plan.targetPlate = plate;
                boolean samePlate = oldVehicle != null
                        && Objects.equals(PlateKeys.normalize(plate), oldVehicle.getPlateKey());
                plan.copyFrom = samePlate ? oldVehicle : null;
                plan.targetModel = plan.requestedModel != null ? plan.requestedModel
                        : (samePlate ? oldVehicle.getModel() : null);
            }
        } else if (ticket.vehicleId != null) {
            throw CheckInException.field("licensePlate", "Phiếu đang gắn xe — chọn một xe của khách hoặc nhập biển số.");
        } else {
            plan.vehicleMode = "NONE";
        }

        if (Objects.equals(target.id, ticket.customerId)
                && Objects.equals(plan.targetVehicleId, ticket.vehicleId)
                && !"NEW".equals(plan.vehicleMode)) {
            throw CheckInException.field("targetCustomerId", "Phiếu đã thuộc đúng khách hàng và xe này — không có gì để chuyển.");
        }

        boolean wantRemove = Boolean.TRUE.equals(request.getRemoveOldVehicle());
        plan.oldVehicle = oldVehicleInfo(ticket, oldVehicle, plan.bookingMovable, plan.targetVehicleId);
        if (wantRemove) {
            if (plan.oldVehicle == null || !plan.oldVehicle.isCanRemove()) {
                throw CheckInException.field("removeOldVehicle",
                        plan.oldVehicle == null ? "Phiếu không gắn xe nào để bỏ."
                                : "Xe " + plan.oldVehicle.getLicensePlate() + " còn " + plan.oldVehicle.getOtherHistory()
                                + " — không bỏ khỏi hồ sơ khách cũ được.");
            }
            plan.removeOldVehicle = true;
            // Xe mới cho B mà xe cũ sắp bỏ: chuyển luôn dòng xe cũ (giữ thông số xe, lịch sử km) thay vì tạo + xoá
            if ("NEW".equals(plan.vehicleMode)) {
                plan.vehicleMode = "TRANSFER";
                plan.targetVehicleId = ticket.vehicleId;
                if (plan.targetModel == null) plan.targetModel = oldVehicle.getModel();
            }
        }
        return plan;
    }

    private ReassignCustomerPreview.OldVehicle oldVehicleInfo(TicketRow ticket, Vehicle oldVehicle,
                                                              boolean bookingMovable, Integer targetVehicleId) {
        if (ticket.vehicleId == null) return null;
        Vehicle v = oldVehicle != null ? oldVehicle : vehicleRepository.findById(ticket.vehicleId).orElse(null);
        if (v == null) return null;
        Integer owner = v.getCustomer() == null ? null : v.getCustomer().getCustomerId();
        boolean ownedByFrom = Objects.equals(owner, ticket.customerId);
        Map<String, Long> other = otherVehicleHistory(ticket, bookingMovable || isBookingMovable(ticket));
        long total = other.values().stream().mapToLong(Long::longValue).sum();
        String desc = other.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> e.getValue() + " " + e.getKey())
                .reduce((a, b) -> a + ", " + b).orElse("");
        boolean canRemove = ownedByFrom && total == 0 && !Objects.equals(targetVehicleId, ticket.vehicleId);
        return ReassignCustomerPreview.OldVehicle.builder()
                .vehicleId(v.getVehicleId())
                .licensePlate(v.getLicensePlate())
                .ownedByFromCustomer(ownedByFrom)
                .otherHistoryCount(total)
                .otherHistory(total == 0 ? "không có lịch sử nào khác" : "lịch sử khác: " + desc)
                .canRemove(canRemove)
                .build();
    }

    /** Lịch sử của xe đang gắn phiếu, KHÔNG tính những dòng đi theo phiếu này (sẽ được chuyển). */
    private Map<String, Long> otherVehicleHistory(TicketRow t, boolean bookingMovable) {
        Map<String, Long> out = new LinkedHashMap<>();
        Integer vid = t.vehicleId;
        int booking = bookingMovable && t.bookingId != null ? t.bookingId : -1;
        count(out, "phiếu dịch vụ", "service_ticket",
                "SELECT COUNT(*) FROM service_ticket WHERE vehicle_id = ? AND service_ticket_id <> ?", vid, t.id);
        count(out, "lịch hẹn", "booking",
                "SELECT COUNT(*) FROM booking WHERE vehicle_id = ? AND booking_id <> ?", vid, booking);
        count(out, "lần ghi số km", "odometer_history",
                "SELECT COUNT(*) FROM odometer_history WHERE vehicle_id = ? AND (service_ticket_id IS NULL OR service_ticket_id <> ?)", vid, t.id);
        count(out, "lượt sổ cũ", "legacy_visit",
                "SELECT COUNT(*) FROM legacy_visit WHERE vehicle_id = ?", vid);
        count(out, "nhắc lịch bảo dưỡng", "service_reminder",
                "SELECT COUNT(*) FROM service_reminder WHERE vehicle_id = ? AND COALESCE(service_ticket_id, -1) <> ?"
                        + " AND COALESCE(booking_id, -1) <> ?", vid, t.id, booking);
        count(out, "bảo hành phụ tùng", "part_warranty",
                "SELECT COUNT(*) FROM part_warranty w WHERE w.vehicle_id = ? AND NOT EXISTS ("
                        + "SELECT 1 FROM estimate_item ei JOIN estimate e ON e.estimate_id = ei.estimate_id"
                        + " WHERE ei.estimate_item_id = w.estimate_item_id AND e.service_ticket_id = ?)", vid, t.id);
        return out;
    }

    private void count(Map<String, Long> out, String label, String table, String sql, Object... args) {
        if (!tableExists(table)) return;
        Long n = jdbc.queryForObject(sql, Long.class, args);
        out.put(label, n == null ? 0 : n);
    }

    /** Lịch hẹn gốc chỉ chuyển theo khi không phiếu nào khác (chưa huỷ) cùng dùng nó. */
    private boolean isBookingMovable(TicketRow t) {
        if (t.bookingId == null) return false;
        Long others = jdbc.queryForObject(
                "SELECT COUNT(*) FROM service_ticket WHERE booking_id = ? AND service_ticket_id <> ?"
                        + " AND ticket_status <> 'CANCELLED'", Long.class, t.bookingId, t.id);
        return others == null || others == 0;
    }

    /**
     * Các bảng sinh ra từ phiếu cần đổi theo. Tham số đầu của mỗi câu UPDATE là customer_id mới,
     * và (nếu bảng có xe) vehicle_id mới — COALESCE để phiếu không gắn xe không xoá xe của dòng đó.
     */
    private List<Move> moves(TicketRow t, boolean bookingMovable) {
        List<Move> list = new ArrayList<>();
        if (bookingMovable && t.bookingId != null) {
            list.add(new Move("booking", "lịch hẹn",
                    "UPDATE booking SET customer_id = ?, vehicle_id = COALESCE(?, vehicle_id) WHERE booking_id = ?",
                    "SELECT COUNT(*) FROM booking WHERE booking_id = ?", true, true, t.bookingId));
        }
        list.add(new Move("odometer_history", "lần ghi số km",
                "UPDATE odometer_history SET vehicle_id = COALESCE(?, vehicle_id) WHERE service_ticket_id = ?",
                "SELECT COUNT(*) FROM odometer_history WHERE service_ticket_id = ?", false, true, t.id));
        int booking = bookingMovable && t.bookingId != null ? t.bookingId : -1;
        list.add(new Move("service_reminder", "nhắc lịch bảo dưỡng",
                "UPDATE service_reminder SET customer_id = ?, vehicle_id = COALESCE(?, vehicle_id)"
                        + " WHERE service_ticket_id = ? OR booking_id = ?",
                "SELECT COUNT(*) FROM service_reminder WHERE service_ticket_id = ? OR booking_id = ?", true, true, t.id, booking));
        String itemsOfTicket = "SELECT ei.estimate_item_id FROM estimate_item ei JOIN estimate e"
                + " ON e.estimate_id = ei.estimate_id WHERE e.service_ticket_id = ?";
        list.add(new Move("part_warranty", "bảo hành phụ tùng",
                "UPDATE part_warranty SET customer_id = ?, vehicle_id = COALESCE(?, vehicle_id)"
                        + " WHERE estimate_item_id IN (" + itemsOfTicket + ")",
                "SELECT COUNT(*) FROM part_warranty WHERE estimate_item_id IN (" + itemsOfTicket + ")", true, true, t.id));
        list.add(new Move("promotion_usage", "lượt dùng khuyến mãi",
                "UPDATE promotion_usage SET customer_id = ? WHERE estimate_id IN"
                        + " (SELECT estimate_id FROM estimate WHERE service_ticket_id = ?)",
                "SELECT COUNT(*) FROM promotion_usage WHERE estimate_id IN"
                        + " (SELECT estimate_id FROM estimate WHERE service_ticket_id = ?)", true, false, t.id));
        return list;
    }

    private List<String> describeMoves(TicketRow t, Plan plan) {
        List<String> out = new ArrayList<>();
        for (Move m : moves(t, plan.bookingMovable)) {
            if (!tableExists(m.table)) continue;
            Long n = jdbc.queryForObject(m.countSql, Long.class, m.keys);
            if (n != null && n > 0) out.add(n + " " + m.label);
        }
        return out;
    }

    // ============================================================================================
    // Xe
    // ============================================================================================

    private Integer createVehicle(Plan plan) {
        CustomerProfile owner = customerRepository.findById(plan.target.id)
                .orElseThrow(() -> CheckInException.field("targetCustomerId", "Không tìm thấy khách hàng đích."));
        Vehicle v = new Vehicle();
        v.setLicensePlate(plan.targetPlate);
        v.setModel(plan.targetModel);
        if (plan.copyFrom != null) {
            v.setBrand(plan.copyFrom.getBrand());
            v.setManufactureYear(plan.copyFrom.getManufactureYear());
        }
        v.setCustomer(owner);
        return vehicleRepository.saveAndFlush(v).getVehicleId();
    }

    /** Chuyển hẳn dòng xe cũ sang khách đích (xe cũ không còn lịch sử nào khác), sửa biển số / kiểu xe nếu đổi. */
    private Integer transferOldVehicle(Integer oldVehicleId, Plan plan) {
        Vehicle v = vehicleRepository.findById(oldVehicleId)
                .orElseThrow(() -> new CheckInException("Không tìm thấy xe đang gắn phiếu."));
        CustomerProfile owner = customerRepository.findById(plan.target.id)
                .orElseThrow(() -> CheckInException.field("targetCustomerId", "Không tìm thấy khách hàng đích."));
        v.setCustomer(owner);
        if (plan.targetPlate != null) v.setLicensePlate(plan.targetPlate);
        if (plan.requestedModel != null) v.setModel(plan.requestedModel);
        return vehicleRepository.saveAndFlush(v).getVehicleId();
    }

    /**
     * Xoá xe nhập nhầm khỏi hồ sơ khách cũ. Thông số kỹ thuật của nó (nếu có) chuyển sang xe đích
     * khi xe đích chưa có, còn lại thì bỏ. Cuối cùng đi qua VehicleService.deleteVehicle — hàm đó
     * đếm lại mọi bảng tham chiếu, nên nếu sót lịch sử nào thì cả lượt chuyển quay lui.
     */
    private void deleteOldVehicle(Integer oldVehicleId, Integer newVehicleId) {
        if (tableExists("vehicle_specification")) {
            if (newVehicleId != null) {
                Long hasSpec = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM vehicle_specification WHERE vehicle_id = ?", Long.class, newVehicleId);
                if (hasSpec == null || hasSpec == 0) {
                    jdbc.update("UPDATE vehicle_specification SET vehicle_id = ? WHERE vehicle_id = ?", newVehicleId, oldVehicleId);
                }
            }
            jdbc.update("DELETE FROM vehicle_specification WHERE vehicle_id = ?", oldVehicleId);
        }
        try {
            vehicleService.deleteVehicle(oldVehicleId);
        } catch (RuntimeException ex) {
            throw CheckInException.field("removeOldVehicle", ex.getMessage());
        }
    }

    // ============================================================================================
    // Đọc dữ liệu
    // ============================================================================================

    private TicketRow requireTicket(Integer serviceTicketId, boolean lock) {
        if (serviceTicketId == null) {
            throw new CheckInException("Thiếu mã phiếu dịch vụ.");
        }
        List<TicketRow> rows = jdbc.query(
                "SELECT service_ticket_id, ticket_code, ticket_status, customer_id, vehicle_id, booking_id, is_deleted,"
                        + " is_walk_in, walk_in_name, walk_in_phone, received_at, completed_at, delivered_at"
                        + " FROM service_ticket WHERE service_ticket_id = ?" + (lock ? " FOR UPDATE" : ""),
                (rs, i) -> {
                    TicketRow t = new TicketRow();
                    t.id = rs.getInt("service_ticket_id");
                    t.code = rs.getString("ticket_code");
                    String status = rs.getString("ticket_status");
                    t.status = status == null ? null : TicketStatus.valueOf(status);
                    t.customerId = (Integer) rs.getObject("customer_id", Integer.class);
                    t.vehicleId = (Integer) rs.getObject("vehicle_id", Integer.class);
                    t.bookingId = (Integer) rs.getObject("booking_id", Integer.class);
                    t.deleted = rs.getBoolean("is_deleted");
                    t.walkIn = rs.getBoolean("is_walk_in");
                    t.walkInName = rs.getString("walk_in_name");
                    t.walkInPhone = rs.getString("walk_in_phone");
                    t.receivedAt = toLdt(rs.getTimestamp("received_at"));
                    t.completedAt = toLdt(rs.getTimestamp("completed_at"));
                    t.deliveredAt = toLdt(rs.getTimestamp("delivered_at"));
                    return t;
                }, serviceTicketId);
        if (rows.isEmpty() || rows.get(0).deleted) {
            throw new CheckInException("Không tìm thấy phiếu dịch vụ #" + serviceTicketId + ".");
        }
        TicketRow t = rows.get(0);
        if (t.status == TicketStatus.CANCELLED) {
            throw new CheckInException("Phiếu " + t.code + " đã bị huỷ — không chuyển khách được.");
        }
        return t;
    }

    private CustomerRow requireTargetCustomer(Integer customerId) {
        List<CustomerRow> rows = jdbc.query(
                "SELECT cp.customer_id, cp.customer_code, cp.full_name, cp.company_name, cp.phone, a.status AS auth_status"
                        + " FROM customer_profile cp LEFT JOIN customer_auth a ON a.customer_id = cp.customer_id"
                        + " WHERE cp.customer_id = ?",
                (rs, i) -> {
                    CustomerRow c = new CustomerRow();
                    c.id = rs.getInt("customer_id");
                    c.code = rs.getString("customer_code");
                    c.fullName = rs.getString("full_name");
                    c.companyName = rs.getString("company_name");
                    c.phone = rs.getString("phone");
                    c.deleted = "DELETED".equalsIgnoreCase(rs.getString("auth_status"));
                    return c;
                }, customerId);
        if (rows.isEmpty() || rows.get(0).deleted) {
            throw CheckInException.field("targetCustomerId", "Không tìm thấy khách hàng #" + customerId + " (hoặc hồ sơ đã bị xoá).");
        }
        CustomerRow c = rows.get(0);
        if (WalkInCustomerService.WALK_IN_CUSTOMER_CODE.equalsIgnoreCase(c.code)) {
            throw CheckInException.field("targetCustomerId", "Không chuyển phiếu sang hồ sơ dùng chung \"Khách lẻ\".");
        }
        return c;
    }

    /** Khách + xe để hiển thị. {@code ticket} khác null thì phiếu vãng lai lấy tên/SĐT ghi trên phiếu. */
    private Party party(Integer customerId, Integer vehicleId, TicketRow ticket) {
        Party p = new Party();
        p.customerId = customerId;
        if (customerId != null) {
            jdbc.query("SELECT customer_code, full_name, company_name, phone FROM customer_profile WHERE customer_id = ?",
                    rs -> {
                        p.customerCode = rs.getString("customer_code");
                        String name = blankToNull(rs.getString("full_name"));
                        p.name = name != null ? name : rs.getString("company_name");
                        p.phone = rs.getString("phone");
                    }, customerId);
        }
        if (ticket != null && ticket.walkIn) {
            p.walkIn = true;
            p.name = ticket.walkInName != null ? ticket.walkInName + " (khách lẻ)" : "Khách lẻ";
            p.phone = ticket.walkInPhone;
        }
        p.vehicleId = vehicleId;
        if (vehicleId != null) {
            jdbc.query("SELECT license_plate, model FROM vehicle WHERE vehicle_id = ?", rs -> {
                p.licensePlate = rs.getString("license_plate");
                p.vehicleModel = rs.getString("model");
            }, vehicleId);
        }
        return p;
    }

    private List<ReassignCustomerPreview.HistoryLine> history(Integer serviceTicketId) {
        if (!tableExists("service_ticket_customer_change")) return List.of();
        return jdbc.query(
                "SELECT c.change_id, c.from_label, c.to_label, c.old_vehicle_action, c.was_paid, c.reason, c.changed_at,"
                        + " sp.full_name AS staff_name"
                        + " FROM service_ticket_customer_change c LEFT JOIN staff_profile sp ON sp.staff_id = c.changed_by"
                        + " WHERE c.service_ticket_id = ? ORDER BY c.change_id DESC",
                (rs, i) -> ReassignCustomerPreview.HistoryLine.builder()
                        .changeId(rs.getInt("change_id"))
                        .fromLabel(rs.getString("from_label"))
                        .toLabel(rs.getString("to_label"))
                        .oldVehicleAction(rs.getString("old_vehicle_action"))
                        .wasPaid(rs.getBoolean("was_paid"))
                        .reason(rs.getString("reason"))
                        .changedAt(toLdt(rs.getTimestamp("changed_at")))
                        .changedByName(rs.getString("staff_name"))
                        .build(),
                serviceTicketId);
    }

    /** Đã thu tiền: phiếu PAID, hoặc còn hoá đơn (chưa VOID) đã thu đủ / thu một phần. */
    private boolean isPaid(TicketRow t) {
        if (t.status == TicketStatus.PAID) return true;
        return billingService.activeBillsOfTicket(t.id).stream()
                .anyMatch(b -> "PAID".equals(b.previousStatus()) || "PARTIALLY_PAID".equals(b.previousStatus()));
    }

    private List<String> baseWarnings(TicketRow t, boolean paid) {
        List<String> w = new ArrayList<>();
        if (paid) {
            w.add("Phiếu đã thu tiền — doanh thu sẽ được tính cho khách mới trong báo cáo khách hàng. "
                    + "Tin nhắn mời đánh giá đã gửi cho khách cũ lúc thanh toán thì không thu hồi được.");
        }
        if (t.walkIn) {
            w.add("Phiếu đang là bán cho khách lẻ — sau khi chuyển, tên/SĐT khách lẻ ghi trên phiếu sẽ được bỏ, "
                    + "phiếu thuộc hẳn hồ sơ khách mới.");
        }
        return w;
    }

    private List<String> warnings(TicketRow t, boolean paid, Plan plan) {
        List<String> w = baseWarnings(t, paid);
        if (t.bookingId != null && !plan.bookingMovable) {
            w.add("Lịch hẹn gốc của phiếu còn dùng chung với phiếu khác nên được giữ nguyên ở khách cũ.");
        }
        if (plan.oldVehicle != null && !plan.removeOldVehicle
                && !"TRANSFER".equals(plan.vehicleMode) && plan.oldVehicle.isCanRemove()) {
            w.add("Xe " + plan.oldVehicle.getLicensePlate() + " sẽ vẫn nằm trong hồ sơ khách cũ (không còn lịch sử nào). "
                    + "Nếu xe này nhập nhầm, hãy tick \"Bỏ xe khỏi hồ sơ khách cũ\".");
        }
        return w;
    }

    private String label(Party p) {
        List<String> parts = new ArrayList<>();
        String name = p.name != null ? p.name : (p.customerId == null ? "?" : "Khách #" + p.customerId);
        parts.add(p.customerCode != null && !p.walkIn ? name + " [" + p.customerCode + "]" : name);
        if (p.phone != null && !p.phone.isBlank()) parts.add(p.phone);
        if (p.licensePlate != null) parts.add("xe " + p.licensePlate);
        return String.join(" · ", parts);
    }

    // ============================================================================================
    // Quyền
    // ============================================================================================

    private Integer requireStaff(StaffPrincipal principal) {
        if (principal == null || principal.getStaffId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        }
        return principal.getStaffId();
    }

    private boolean hasPaidReassignRole(StaffPrincipal principal) {
        List<String> roles = principal == null ? null : principal.getRoleCodes();
        return roles != null && roles.stream()
                .map(r -> r == null ? "" : r.trim().toUpperCase())
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .anyMatch(PAID_REASSIGN_ROLES::contains);
    }

    private static String paidBlockedMessage() {
        return "Phiếu đã thu tiền — chỉ Admin hoặc Quản lý mới được chuyển sang khách khác.";
    }

    // ============================================================================================
    // Tiện ích
    // ============================================================================================

    private final Map<String, Boolean> tableCache = new java.util.concurrent.ConcurrentHashMap<>();

    /** Vài bảng (part_warranty, vehicle_specification...) chỉ có từ changeset 010 — DB cũ có thể chưa có. */
    private boolean tableExists(String table) {
        return tableCache.computeIfAbsent(table, t -> {
            Long n = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                    Long.class, t);
            return n != null && n > 0;
        });
    }

    private void refreshDashboardAfterCommit(Set<LocalDate> dates) {
        if (dates.isEmpty()) return;
        TransactionTemplate newTx = new TransactionTemplate(transactionManager);
        newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Runnable refresh = () -> {
            for (LocalDate d : dates) {
                try {
                    newTx.executeWithoutResult(status -> dashboardSummaryService.refresh(d, d));
                } catch (RuntimeException ex) {
                    log.error("Chuyển phiếu sang khách khác: không tính lại được dashboard ngày {}", d, ex);
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
        if (value != null) dates.add(value.toLocalDate());
    }

    private static LocalDateTime toLdt(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // ============================================================================================

    private static final class TicketRow {
        Integer id;
        String code;
        TicketStatus status;
        Integer customerId;
        Integer vehicleId;
        Integer bookingId;
        boolean deleted;
        boolean walkIn;
        String walkInName;
        String walkInPhone;
        LocalDateTime receivedAt;
        LocalDateTime completedAt;
        LocalDateTime deliveredAt;
    }

    private static final class CustomerRow {
        Integer id;
        String code;
        String fullName;
        String companyName;
        String phone;
        boolean deleted;

        String displayName() {
            return fullName != null && !fullName.isBlank() ? fullName : (companyName != null ? companyName : "#" + id);
        }

        Integer getCustomerId() {
            return id;
        }
    }

    private static final class Party {
        Integer customerId;
        String customerCode;
        String name;
        String phone;
        boolean walkIn;
        Integer vehicleId;
        String licensePlate;
        String vehicleModel;

        ReassignCustomerPreview.Party dto() {
            return ReassignCustomerPreview.Party.builder()
                    .customerId(customerId).customerCode(customerCode).name(name).phone(phone).walkIn(walkIn)
                    .vehicleId(vehicleId).licensePlate(licensePlate).vehicleModel(vehicleModel)
                    .build();
        }
    }

    private static final class Plan {
        CustomerRow target;
        /** EXISTING / NEW / TRANSFER / NONE */
        String vehicleMode;
        Integer targetVehicleId;
        String targetPlate;
        String targetModel;
        String requestedModel;
        Vehicle copyFrom;
        boolean bookingMovable;
        boolean removeOldVehicle;
        ReassignCustomerPreview.OldVehicle oldVehicle;

        Party toParty() {
            Party p = new Party();
            p.customerId = target.id;
            p.customerCode = target.code;
            p.name = target.displayName();
            p.phone = target.phone;
            p.vehicleId = targetVehicleId;
            p.licensePlate = targetPlate;
            p.vehicleModel = targetModel;
            return p;
        }
    }

    /**
     * Một bảng đi theo phiếu. {@code keys} là tham số khoá (sau customer_id / vehicle_id mới) của câu
     * UPDATE, và là toàn bộ tham số của câu đếm.
     */
    private record Move(String table, String label, String updateSql, String countSql,
                        boolean setsCustomer, boolean setsVehicle, Object... keys) {

        Object[] updateArgs(Integer customerId, Integer vehicleId) {
            List<Object> args = new ArrayList<>();
            if (setsCustomer) args.add(customerId);
            if (setsVehicle) args.add(vehicleId);
            args.addAll(List.of(keys));
            return args.toArray();
        }
    }
}
