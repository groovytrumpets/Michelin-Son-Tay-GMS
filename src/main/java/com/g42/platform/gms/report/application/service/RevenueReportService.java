package com.g42.platform.gms.report.application.service;

import com.g42.platform.gms.auth.api.internal.CustomerInternalApi;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.billing.domain.enums.PaymentStatus;
import com.g42.platform.gms.billing.infrastructure.entity.ServiceBillJpa;
import com.g42.platform.gms.billing.infrastructure.repository.ServiceBillJpaRepo;
import com.g42.platform.gms.customerimport.infrastructure.entity.LegacyVisitJpa;
import com.g42.platform.gms.customerimport.infrastructure.repository.LegacyVisitRepository;
import com.g42.platform.gms.report.api.dto.RevenueReportResponse;
import com.g42.platform.gms.report.api.dto.RevenueReportResponse.Line;
import com.g42.platform.gms.report.api.dto.RevenueReportResponse.Summary;
import com.g42.platform.gms.report.api.dto.RevenueReportResponse.Transaction;
import com.g42.platform.gms.service_ticket_management.domain.enums.BackfillReviewStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketType;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.vehicle.api.internal.VehicleInternalApi;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Dữ liệu cho trang Quản lý doanh thu (/revenue-management). Cùng nguồn với báo cáo khách hàng
 * ({@link CustomerReportService}) nhưng lấy hoá đơn làm trung tâm:
 *  - {@code service_bill} đã thu có {@code paid_at} trong kỳ → doanh thu đã thu, tính theo ngày thu.
 *  - {@code service_bill} chưa thu của phiếu tiếp nhận trong kỳ → công nợ chưa thu.
 *  - {@code payment_transaction} → phương thức thu tiền.
 *  - {@code service_ticket_assignment} (cố vấn) → nhân viên phụ trách.
 *  - {@code estimate_item} của báo giá gắn với hoá đơn → phân rã theo hạng mục / dịch vụ-phụ tùng.
 *
 * Phiếu chưa có hoá đơn không phải là doanh thu nên không có mặt ở đây.
 */
@Service
@RequiredArgsConstructor
public class RevenueReportService {

    /** Hoá đơn chưa thu sau ngần này ngày kể từ lúc tiếp nhận phiếu thì coi là quá hạn. */
    public static final int OVERDUE_AFTER_DAYS = 7;

    private static final String SOURCE_SYSTEM = "SYSTEM";
    private static final String SOURCE_LEGACY = "LEGACY";
    private static final String NO_CATEGORY = "Chưa phân loại";
    private static final String NO_STAFF = "Chưa phân công";
    private static final String LEGACY_LABEL = "Sổ dịch vụ cũ";

    private final ServiceBillJpaRepo serviceBillJpaRepo;
    private final ServiceTicketRepository serviceTicketRepository;
    private final LegacyVisitRepository legacyVisitRepository;
    private final CustomerInternalApi customerInternalApi;
    private final VehicleInternalApi vehicleInternalApi;
    private final EntityManager entityManager;
    private final com.g42.platform.gms.branch.service.BranchDirectory branchDirectory;

    @Transactional(readOnly = true)
    public RevenueReportResponse buildReport(LocalDate fromDate, LocalDate toDate, boolean includeLegacy) {
        return buildReport(fromDate, toDate, includeLegacy, null);
    }

    /**
     * @param branchId null = mọi xưởng. Sổ dịch vụ cũ không ghi xưởng, nhưng toàn bộ là của
     *                 xưởng có sẵn trước khi có nhiều xưởng (xưởng mặc định) nên chỉ hiện khi
     *                 lọc đúng xưởng đó.
     */
    @Transactional(readOnly = true)
    public RevenueReportResponse buildReport(LocalDate fromDate, LocalDate toDate, boolean includeLegacy,
                                             Integer branchId) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1);
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        if (from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        ZoneId zone = ZoneId.systemDefault();

        // 1) Hoá đơn: đã thu trong kỳ + hoá đơn của phiếu tiếp nhận trong kỳ.
        Map<Integer, ServiceBillJpa> billsById = new LinkedHashMap<>();
        serviceBillJpaRepo.findByPaidAtBetween(start.atZone(zone).toInstant(), end.atZone(zone).toInstant())
                .forEach(b -> billsById.put(b.getBillId(), b));

        Map<Integer, ServiceTicketJpa> ticketsById = new HashMap<>();
        serviceTicketRepository.findServiceTicketJpasByReceivedAtBetween(start, end)
                .forEach(t -> ticketsById.put(t.getServiceTicketId(), t));
        if (!ticketsById.isEmpty()) {
            serviceBillJpaRepo.findByServiceTicketIdIn(ticketsById.keySet())
                    .forEach(b -> billsById.putIfAbsent(b.getBillId(), b));
        }

        Set<Integer> missingTicketIds = billsById.values().stream()
                .map(ServiceBillJpa::getServiceTicketId)
                .filter(id -> id != null && !ticketsById.containsKey(id))
                .collect(Collectors.toSet());
        if (!missingTicketIds.isEmpty()) {
            serviceTicketRepository.findAllById(missingTicketIds)
                    .forEach(t -> ticketsById.put(t.getServiceTicketId(), t));
        }

        // Mỗi phiếu chỉ lấy một hoá đơn (ưu tiên hoá đơn đã thu mới nhất), bỏ phiếu đã xoá / huỷ.
        Map<Integer, ServiceBillJpa> billByTicket = new HashMap<>();
        for (ServiceBillJpa bill : billsById.values()) {
            ServiceTicketJpa ticket = ticketsById.get(bill.getServiceTicketId());
            if (ticket == null || !isCountable(ticket)) {
                continue;
            }
            if (branchId != null && !branchId.equals(ticket.getBranchId())) {
                continue;
            }
            billByTicket.merge(ticket.getServiceTicketId(), bill,
                    (a, b) -> preferBill(b, a) ? b : a);
        }

        List<ServiceBillJpa> bills = new ArrayList<>(billByTicket.values());
        List<ServiceTicketJpa> tickets = bills.stream()
                .map(b -> ticketsById.get(b.getServiceTicketId())).toList();

        Integer defaultBranchId = branchDirectory.defaultBranchId();
        boolean legacyInBranch = branchId == null || branchId.equals(defaultBranchId);
        List<LegacyVisitJpa> legacyVisits = includeLegacy && legacyInBranch
                ? legacyVisitRepository.findByVisitedAtBetweenOrderByVisitedAtAsc(start, end).stream()
                    .filter(v -> nvl(v.getTotalAmount()).signum() > 0)
                    .toList()
                : List.of();

        // 2) Tra cứu hàng loạt: khách, xe, phương thức thu, nhân viên, dòng báo giá.
        Map<Integer, CustomerProfile> customerMap = loadCustomers(Stream.concat(
                tickets.stream().map(ServiceTicketJpa::getCustomerId),
                legacyVisits.stream().map(LegacyVisitJpa::getCustomerId)));
        Map<Integer, Vehicle> vehicleMap = loadVehicles(Stream.concat(
                tickets.stream().map(ServiceTicketJpa::getVehicleId),
                legacyVisits.stream().map(LegacyVisitJpa::getVehicleId)));
        List<Integer> billIds = bills.stream().map(ServiceBillJpa::getBillId).toList();
        List<Integer> ticketIds = tickets.stream().map(ServiceTicketJpa::getServiceTicketId).toList();
        Map<Integer, String> methodByBill = loadPaymentMethods(billIds);
        Map<Integer, String> advisorByTicket = loadAdvisorNames(ticketIds);
        Map<Integer, String> staffNames = loadStaffNames(tickets.stream()
                .flatMap(t -> Stream.of(t.getBackfillAdvisorId(), t.getCreatedBy())));
        Map<Integer, List<RawLine>> linesByEstimate = loadEstimateLines(bills.stream()
                .map(ServiceBillJpa::getEstimateId).filter(Objects::nonNull).distinct().toList());

        // 3) Dựng giao dịch.
        LocalDate today = LocalDate.now();
        List<Transaction> transactions = new ArrayList<>(bills.size() + legacyVisits.size());

        for (ServiceBillJpa bill : bills) {
            ServiceTicketJpa ticket = ticketsById.get(bill.getServiceTicketId());
            CustomerProfile customer = ticket.getCustomerId() != null ? customerMap.get(ticket.getCustomerId()) : null;
            Vehicle vehicle = ticket.getVehicleId() != null ? vehicleMap.get(ticket.getVehicleId()) : null;

            boolean paid = bill.getPaymentStatus() == PaymentStatus.PAID;
            LocalDateTime paidAt = paid && bill.getPaidAt() != null
                    ? LocalDateTime.ofInstant(bill.getPaidAt(), zone) : null;
            LocalDate receivedDate = ticket.getReceivedAt() != null ? ticket.getReceivedAt().toLocalDate() : null;
            LocalDate date = paidAt != null ? paidAt.toLocalDate() : (receivedDate != null ? receivedDate : from);
            // Hoá đơn đã thu ngoài kỳ nhưng phiếu tiếp nhận trong kỳ thì thuộc kỳ khác.
            if (date.isBefore(from) || date.isAfter(to)) {
                continue;
            }

            String status;
            if (paid) {
                status = "PAID";
            } else if (receivedDate != null && receivedDate.plusDays(OVERDUE_AFTER_DAYS).isBefore(today)) {
                status = "OVERDUE";
            } else {
                status = "UNPAID";
            }

            BigDecimal total = nvl(bill.getFinalAmount());
            List<Line> lines = prorate(linesByEstimate.getOrDefault(bill.getEstimateId(), List.of()), total);

            String method = methodByBill.get(bill.getBillId());
            if (method == null && paid) {
                method = nz(ticket.getBackfillPaymentMethod());
            }

            transactions.add(Transaction.builder()
                    .id("B-" + bill.getBillId())
                    .serviceTicketId(ticket.getServiceTicketId())
                    .billId(bill.getBillId())
                    .ticketCode(nz(ticket.getTicketCode()))
                    .customerName(customer != null && !nz(customer.getFullName()).isBlank() ? customer.getFullName() : "Khách lẻ")
                    .customerPhone(customer != null ? nz(customer.getPhone()) : "")
                    .licensePlate(vehicle != null ? nz(vehicle.getLicensePlate()) : "")
                    .date(date)
                    .receivedAt(ticket.getReceivedAt())
                    .paidAt(paidAt)
                    .type(ticket.getTicketType() == TicketType.PARTS_SALE ? "PART" : "SERVICE")
                    .ticketStatus(ticket.getTicketStatus() != null ? ticket.getTicketStatus().name() : "")
                    .category(mainCategory(lines))
                    .staffName(resolveStaff(ticket, advisorByTicket, staffNames))
                    .branchName(branchDirectory.nameOf(ticket.getBranchId()))
                    .subtotal(bill.getSubTotal() != null ? bill.getSubTotal() : total.add(nvl(bill.getDiscountAmount())))
                    .discountAmount(nvl(bill.getDiscountAmount()))
                    .totalAmount(total)
                    .paymentMethod(paid ? nz(method) : "")
                    .status(status)
                    .source(SOURCE_SYSTEM)
                    .lines(lines)
                    .build());
        }

        for (LegacyVisitJpa visit : legacyVisits) {
            CustomerProfile customer = visit.getCustomerId() != null ? customerMap.get(visit.getCustomerId()) : null;
            Vehicle vehicle = visit.getVehicleId() != null ? vehicleMap.get(visit.getVehicleId()) : null;
            BigDecimal amount = nvl(visit.getTotalAmount());
            BigDecimal discount = nvl(visit.getDiscountAmount());
            String code = nz(visit.getLegacyTicketCode()).trim();

            transactions.add(Transaction.builder()
                    .id("L-" + visit.getLegacyVisitId())
                    .legacyVisitId(visit.getLegacyVisitId())
                    .ticketCode(!code.isEmpty() ? code : "SC-" + visit.getLegacyVisitId())
                    .customerName(customer != null && !nz(customer.getFullName()).isBlank() ? customer.getFullName() : "Khách lẻ")
                    .customerPhone(customer != null ? nz(customer.getPhone()) : "")
                    .licensePlate(vehicle != null ? nz(vehicle.getLicensePlate()) : "")
                    .date(visit.getVisitedAt() != null ? visit.getVisitedAt().toLocalDate() : from)
                    .receivedAt(visit.getVisitedAt())
                    .paidAt(visit.getVisitedAt())
                    .type(SOURCE_LEGACY)
                    .ticketStatus(SOURCE_LEGACY)
                    .category(LEGACY_LABEL)
                    .staffName(LEGACY_LABEL)
                    .branchName(branchDirectory.nameOf(defaultBranchId))
                    .subtotal(amount.add(discount))
                    .discountAmount(discount)
                    .totalAmount(amount)
                    .paymentMethod(SOURCE_LEGACY)
                    // Sổ cũ chỉ ghi lượt đã làm xong: có tiền trong sổ nghĩa là đã thu.
                    .status("PAID")
                    .source(SOURCE_LEGACY)
                    .lines(List.of(Line.builder().category(LEGACY_LABEL).lineType(SOURCE_LEGACY).amount(amount).build()))
                    .build());
        }

        transactions.sort(Comparator.comparing(Transaction::getDate).reversed()
                .thenComparing(Transaction::getTicketCode, Comparator.nullsLast(Comparator.reverseOrder())));

        return RevenueReportResponse.builder()
                .summary(summarize(from, to, includeLegacy, transactions))
                .transactions(transactions)
                .build();
    }

    // ------------------------------------------------------------------------------------------

    private static Summary summarize(LocalDate from, LocalDate to, boolean includeLegacy, List<Transaction> rows) {
        BigDecimal paid = sum(rows, "PAID");
        BigDecimal unpaid = sum(rows, "UNPAID");
        BigDecimal overdue = sum(rows, "OVERDUE");
        List<Transaction> legacy = rows.stream().filter(t -> SOURCE_LEGACY.equals(t.getSource())).toList();
        return Summary.builder()
                .from(from)
                .to(to)
                .subtotal(rows.stream().map(Transaction::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add))
                .discountAmount(rows.stream().map(Transaction::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalRevenue(paid.add(unpaid).add(overdue))
                .paidRevenue(paid)
                .unpaidRevenue(unpaid)
                .overdueRevenue(overdue)
                .invoiceCount(rows.size())
                .paidCount(rows.stream().filter(t -> "PAID".equals(t.getStatus())).count())
                .overdueAfterDays(OVERDUE_AFTER_DAYS)
                .legacyIncluded(includeLegacy)
                .legacyCount(legacy.size())
                .legacyRevenue(legacy.stream().map(Transaction::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .build();
    }

    private static BigDecimal sum(List<Transaction> rows, String status) {
        return rows.stream().filter(t -> status.equals(t.getStatus()))
                .map(Transaction::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Phiếu có được tính vào doanh thu không — dashboard dùng chung quy tắc này. */
    public static boolean isCountable(ServiceTicketJpa ticket) {
        return !Boolean.TRUE.equals(ticket.getIsDeleted())
                && ticket.getTicketStatus() != TicketStatus.CANCELLED
                && ticket.getBackfillReviewStatus() != BackfillReviewStatus.REJECTED;
    }

    /** Ưu tiên hoá đơn đã thu, rồi hoá đơn thu muộn hơn, rồi mã hoá đơn lớn hơn. */
    public static boolean preferBill(ServiceBillJpa candidate, ServiceBillJpa current) {
        boolean candPaid = candidate.getPaymentStatus() == PaymentStatus.PAID;
        boolean currPaid = current.getPaymentStatus() == PaymentStatus.PAID;
        if (candPaid != currPaid) {
            return candPaid;
        }
        Instant a = candidate.getPaidAt();
        Instant b = current.getPaidAt();
        if (a != null && b != null && !a.equals(b)) {
            return a.isAfter(b);
        }
        return Objects.requireNonNullElse(candidate.getBillId(), 0) > Objects.requireNonNullElse(current.getBillId(), 0);
    }

    private static String resolveStaff(ServiceTicketJpa ticket, Map<Integer, String> advisorByTicket,
                                       Map<Integer, String> staffNames) {
        String advisor = advisorByTicket.get(ticket.getServiceTicketId());
        if (advisor != null && !advisor.isBlank()) {
            return advisor;
        }
        for (Integer staffId : new Integer[]{ticket.getBackfillAdvisorId(), ticket.getCreatedBy()}) {
            String name = staffId != null ? staffNames.get(staffId) : null;
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return NO_STAFF;
    }

    /**
     * Gom dòng báo giá theo hạng mục rồi chia tỉ lệ cho khớp tổng hoá đơn — hoá đơn còn giảm giá
     * / khuyến mãi cấp phiếu nên tổng dòng thường lệch với số thực thu.
     */
    private static List<Line> prorate(List<RawLine> raw, BigDecimal total) {
        Map<String, RawLine> grouped = new LinkedHashMap<>();
        for (RawLine line : raw) {
            grouped.merge(line.category + "|" + line.lineType, line,
                    (a, b) -> new RawLine(a.category, a.lineType, a.amount.add(b.amount)));
        }
        BigDecimal lineSum = grouped.values().stream().map(l -> l.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lineSum.signum() <= 0) {
            return total.signum() == 0 ? List.of()
                    : List.of(Line.builder().category(NO_CATEGORY).lineType("OTHER").amount(total).build());
        }
        List<Line> result = new ArrayList<>(grouped.size());
        BigDecimal allocated = BigDecimal.ZERO;
        int i = 0;
        for (RawLine line : grouped.values()) {
            i++;
            BigDecimal amount = i == grouped.size()
                    ? total.subtract(allocated)
                    : total.multiply(line.amount).divide(lineSum, 0, RoundingMode.HALF_UP);
            allocated = allocated.add(amount);
            result.add(Line.builder().category(line.category).lineType(line.lineType).amount(amount).build());
        }
        result.sort(Comparator.comparing(Line::getAmount).reversed());
        return result;
    }

    private static String mainCategory(List<Line> lines) {
        return lines.isEmpty() ? NO_CATEGORY : lines.get(0).getCategory();
    }

    // ------------------------------------------------------------------------------------------
    // Tra cứu hàng loạt

    private Map<Integer, CustomerProfile> loadCustomers(Stream<Integer> ids) {
        List<Integer> list = ids.filter(Objects::nonNull).distinct().toList();
        if (list.isEmpty()) {
            return Map.of();
        }
        return customerInternalApi.findAllByIds(list).stream()
                .filter(c -> c != null && c.getCustomerId() != null)
                .collect(Collectors.toMap(CustomerProfile::getCustomerId, c -> c, (a, b) -> a));
    }

    private Map<Integer, Vehicle> loadVehicles(Stream<Integer> ids) {
        List<Integer> list = ids.filter(Objects::nonNull).distinct().toList();
        if (list.isEmpty()) {
            return Map.of();
        }
        return vehicleInternalApi.findAllByIds(list).stream()
                .filter(v -> v != null && v.getVehicleId() != null)
                .collect(Collectors.toMap(Vehicle::getVehicleId, v -> v, (a, b) -> a));
    }

    /** Phương thức của giao dịch thu thành công gần nhất trên mỗi hoá đơn. */
    private Map<Integer, String> loadPaymentMethods(Collection<Integer> billIds) {
        Map<Integer, String> result = new HashMap<>();
        if (billIds.isEmpty()) {
            return result;
        }
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT bill_id, method FROM payment_transaction
                        WHERE bill_id IN (:ids) AND (status IS NULL OR status = 'SUCCESS')
                        ORDER BY paid_at, transaction_id
                        """)
                .setParameter("ids", billIds)
                .getResultList();
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            String method = str(r[1]);
            if (!method.isBlank()) {
                result.put(toInt(r[0]), method.trim().toUpperCase());
            }
        }
        return result;
    }

    /** Cố vấn dịch vụ của phiếu (ưu tiên người chính). */
    private Map<Integer, String> loadAdvisorNames(Collection<Integer> ticketIds) {
        Map<Integer, String> result = new HashMap<>();
        if (ticketIds.isEmpty()) {
            return result;
        }
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT a.service_ticket_id, s.full_name
                        FROM service_ticket_assignment a
                        JOIN staff_profile s ON s.staff_id = a.staff_id
                        WHERE a.service_ticket_id IN (:ids) AND a.role_in_ticket = 'ADVISOR'
                        ORDER BY COALESCE(a.is_primary, 0) DESC, a.assignment_id
                        """)
                .setParameter("ids", ticketIds)
                .getResultList();
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.putIfAbsent(toInt(r[0]), str(r[1]));
        }
        return result;
    }

    private Map<Integer, String> loadStaffNames(Stream<Integer> ids) {
        Set<Integer> set = ids.filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        Map<Integer, String> result = new HashMap<>();
        if (set.isEmpty()) {
            return result;
        }
        List<?> rows = entityManager.createNativeQuery(
                        "SELECT staff_id, full_name FROM staff_profile WHERE staff_id IN (:ids)")
                .setParameter("ids", set)
                .getResultList();
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.put(toInt(r[0]), str(r[1]));
        }
        return result;
    }

    /** Dòng báo giá được chốt (đã tick, chưa gỡ) kèm hạng mục và loại hàng. */
    private Map<Integer, List<RawLine>> loadEstimateLines(Collection<Integer> estimateIds) {
        Map<Integer, List<RawLine>> result = new HashMap<>();
        if (estimateIds.isEmpty()) {
            return result;
        }
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT ei.estimate_id, ei.final_price, ei.category_label,
                               ic.category_name, ic.category_type, ci.item_type
                        FROM estimate_item ei
                        LEFT JOIN catalog_item ci ON ci.item_id = ei.item_id
                        LEFT JOIN item_category ic
                               ON ic.item_category_id = COALESCE(ei.item_category_id, ci.item_category_id)
                        WHERE ei.estimate_id IN (:ids)
                          AND COALESCE(ei.is_checked, 0) = 1
                          AND COALESCE(ei.is_removed, 0) = 0
                        """)
                .setParameter("ids", estimateIds)
                .getResultList();
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            BigDecimal amount = toDecimal(r[1]);
            String label = str(r[2]).trim();
            String categoryName = str(r[3]).trim();
            String category = !categoryName.isEmpty() ? categoryName : (!label.isEmpty() ? label : NO_CATEGORY);
            String lineType = lineType(str(r[5]), str(r[4]));
            result.computeIfAbsent(toInt(r[0]), k -> new ArrayList<>())
                    .add(new RawLine(category, lineType, amount));
        }
        return result;
    }

    /** Loại hàng theo catalog_item.item_type, thiếu thì đoán theo item_category.category_type. */
    private static String lineType(String itemType, String categoryType) {
        switch (itemType.trim().toUpperCase()) {
            case "SERVICE", "COMBO", "MAINTENANCE_PACKAGE":
                return "SERVICE";
            case "PART", "EQUIPMENT", "MACHINERY":
                return "PART";
            default:
                break;
        }
        String ct = categoryType.trim().toUpperCase();
        if (ct.startsWith("SERV")) {
            return "SERVICE";
        }
        if (ct.startsWith("PART")) {
            return "PART";
        }
        return "OTHER";
    }

    private record RawLine(String category, String lineType, BigDecimal amount) {
    }

    private static int toInt(Object value) {
        return value instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private static BigDecimal toDecimal(Object value) {
        if (value instanceof BigDecimal b) {
            return b;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private static String str(Object value) {
        return value != null ? value.toString() : "";
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static String nz(String value) {
        return value != null ? value : "";
    }
}
