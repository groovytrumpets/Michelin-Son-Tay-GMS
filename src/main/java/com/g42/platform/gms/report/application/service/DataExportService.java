package com.g42.platform.gms.report.application.service;

import com.g42.platform.gms.billing.domain.enums.PaymentStatus;
import com.g42.platform.gms.billing.infrastructure.entity.ServiceBillJpa;
import com.g42.platform.gms.billing.infrastructure.repository.ServiceBillJpaRepo;
import com.g42.platform.gms.branch.service.BranchDirectory;
import com.g42.platform.gms.customerimport.infrastructure.entity.LegacyVisitJpa;
import com.g42.platform.gms.customerimport.infrastructure.repository.LegacyVisitRepository;
import com.g42.platform.gms.report.application.service.export.DataExportOptions;
import com.g42.platform.gms.report.application.service.export.DataExportOptions.DateBasis;
import com.g42.platform.gms.report.application.service.export.DataExportType;
import com.g42.platform.gms.report.application.service.export.XlsxWriter;
import com.g42.platform.gms.report.application.service.export.XlsxWriter.Col;
import com.g42.platform.gms.report.application.service.export.XlsxWriter.Fmt;
import com.g42.platform.gms.service_ticket_management.domain.enums.EntryMode;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketType;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Báo cáo tổng hợp (/report-export): một file Excel, mỗi loại dữ liệu một hoặc vài tab.
 *
 * <p>Quy ước số liệu — cố ý giống các báo cáo đã có để số khớp nhau:
 * <ul>
 *   <li>Phiếu vào kỳ theo {@link DateBasis}: ngày tiếp nhận (như /customer-report) hoặc ngày thu
 *       tiền của hoá đơn (khớp sổ quỹ). Bỏ phiếu đã xoá; phiếu huỷ / nhập bù bị từ chối vẫn liệt kê
 *       nhưng KHÔNG tính tiền (quy tắc {@link RevenueReportService#isCountable}).</li>
 *   <li>Mỗi phiếu một hoá đơn ({@link RevenueReportService#preferBill}). Phiếu chưa có hoá đơn
 *       không phải doanh thu. Hoá đơn VOID / REFUNDED không tính.</li>
 *   <li>Doanh thu theo sản phẩm = tiền hoá đơn chia lại cho từng dòng báo giá theo tỉ lệ thành
 *       tiền (hoá đơn còn giảm giá cấp phiếu), nên cộng tab sản phẩm = cộng tab phiếu.</li>
 *   <li>Lợi nhuận gộp = doanh thu CHƯA VAT − giá vốn lô đã xuất kho (stock_issue CONFIRMED) − tiền
 *       công thuê ngoài − hoa hồng / điều chỉnh. Hàng chưa xuất kho hoặc xuất không có lô thì giá
 *       vốn thiếu → cột ghi chú báo "giá vốn chưa đủ", không tự đoán giá.</li>
 *   <li>Sổ dịch vụ cũ ở tab riêng, tiền KHÔNG cộng vào doanh thu hoá đơn (tiền chép tay).</li>
 *   <li>Tồn kho và danh bạ là số tại thời điểm xuất, không theo kỳ; tồn kho không lọc xưởng vì
 *       kho dùng chung.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportService {

    private static final int CHUNK = 900;
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String NO_CATEGORY = "Chưa phân loại";
    private static final String WALK_IN = "Khách lẻ";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FILE_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<String> PHYSICAL_TYPES = Set.of("PART", "EQUIPMENT", "MACHINERY");

    private final ServiceTicketRepository serviceTicketRepository;
    private final ServiceBillJpaRepo serviceBillJpaRepo;
    private final LegacyVisitRepository legacyVisitRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final BranchDirectory branchDirectory;

    /** Người xuất — in vào tab Tổng quan. */
    public record Actor(String name, String maskedEmail) {}

    /** @param sheetRows số dòng dữ liệu từng tab (không tính dòng tiêu đề) */
    public record Result(byte[] content, String filename, Map<String, Integer> sheetRows, String summary) {}

    @Transactional(readOnly = true)
    public Result export(DataExportOptions o, Actor actor) throws IOException {
        long startedAt = System.currentTimeMillis();
        boolean needScope = o.has(DataExportType.CUSTOMERS) || o.has(DataExportType.PRODUCT_REVENUE)
                || o.has(DataExportType.TICKETS) || o.has(DataExportType.PROFIT);
        Scope scope = needScope ? loadScope(o) : new Scope();
        List<LegacyVisitJpa> legacy = o.includeLegacy()
                && (o.has(DataExportType.CUSTOMERS) || o.has(DataExportType.TICKETS))
                ? loadLegacy(o) : List.of();
        loadPeople(scope, legacy);

        Map<String, Integer> sheetRows = new LinkedHashMap<>();
        Overview ov = new Overview();
        byte[] content;
        try (XlsxWriter x = new XlsxWriter()) {
            // Tạo trước để là tab đầu tiên; điền sau cùng khi đã có số liệu các tab khác.
            XlsxWriter.KeyValue overview = x.keyValue("Tổng quan");

            if (o.has(DataExportType.CUSTOMERS)) {
                writeCustomers(x, o, scope, legacy, sheetRows, ov);
            }
            List<ProductAgg> products = o.has(DataExportType.PRODUCT_REVENUE) || o.has(DataExportType.PROFIT)
                    ? aggregateProducts(scope) : List.of();
            if (o.has(DataExportType.PRODUCT_REVENUE)) {
                writeProductRevenue(x, products, sheetRows);
            }
            if (o.has(DataExportType.INVENTORY)) {
                writeInventory(x, o, sheetRows, ov);
            }
            if (o.has(DataExportType.TICKETS)) {
                writeTickets(x, scope, sheetRows);
                writeTicketLines(x, scope, sheetRows);
                if (o.includeLegacy()) {
                    writeLegacy(x, scope, legacy, sheetRows);
                }
            }
            if (o.has(DataExportType.PROFIT)) {
                writeProfitByTicket(x, scope, sheetRows);
                writeProfitByProduct(x, products, sheetRows);
            }
            writeOverview(overview, o, actor, scope, legacy, sheetRows, ov);
            content = x.toBytes();
        }

        String filename = "Bao-cao-tong-hop_" + o.from().format(FILE_DAY) + "-" + o.to().format(FILE_DAY) + ".xlsx";
        String summary = sheetRows.entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue())
                .collect(Collectors.joining(", "));
        log.info("Xuất báo cáo tổng hợp {} ({} byte, {} ms): {}", filename, content.length,
                System.currentTimeMillis() - startedAt, summary);
        return new Result(content, filename, sheetRows, summary);
    }

    // ==========================================================================================
    // Nạp dữ liệu phiếu của kỳ

    private Scope loadScope(DataExportOptions o) {
        Scope s = new Scope();
        s.loaded = true;
        LocalDateTime start = o.from().atStartOfDay();
        LocalDateTime end = o.to().atTime(LocalTime.MAX);

        Map<Integer, ServiceTicketJpa> tickets = new LinkedHashMap<>();
        Map<Integer, ServiceBillJpa> billByTicket = new HashMap<>();
        if (o.basis() == DateBasis.PAID) {
            List<ServiceBillJpa> paidBills = serviceBillJpaRepo
                    .findByPaidAtBetween(start.atZone(ZONE).toInstant(), end.atZone(ZONE).toInstant()).stream()
                    .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID && b.getServiceTicketId() != null)
                    .toList();
            inChunks(paidBills.stream().map(ServiceBillJpa::getServiceTicketId).toList(),
                    chunk -> serviceTicketRepository.findAllById(chunk)
                            .forEach(t -> tickets.put(t.getServiceTicketId(), t)));
            paidBills.forEach(b -> billByTicket.merge(b.getServiceTicketId(), b, DataExportService::pickBill));
        } else {
            serviceTicketRepository.findServiceTicketJpasByReceivedAtBetween(start, end)
                    .forEach(t -> tickets.put(t.getServiceTicketId(), t));
            inChunks(tickets.keySet(), chunk -> serviceBillJpaRepo.findByServiceTicketIdIn(chunk).stream()
                    .filter(b -> b.getPaymentStatus() != PaymentStatus.VOID && b.getServiceTicketId() != null)
                    .forEach(b -> billByTicket.merge(b.getServiceTicketId(), b, DataExportService::pickBill)));
        }
        tickets.values().removeIf(t -> Boolean.TRUE.equals(t.getIsDeleted())
                || (o.branchId() != null && !o.branchId().equals(t.getBranchId())));
        if (tickets.isEmpty()) {
            return s;
        }

        // Dòng hàng: báo giá của hoá đơn; phiếu chưa có hoá đơn thì lấy báo giá phiên bản mới nhất.
        Map<Integer, Integer> estimateByTicket = new HashMap<>();
        List<Integer> withoutBill = new ArrayList<>();
        for (ServiceTicketJpa t : tickets.values()) {
            ServiceBillJpa bill = billByTicket.get(t.getServiceTicketId());
            if (bill != null && bill.getEstimateId() != null) {
                estimateByTicket.put(t.getServiceTicketId(), bill.getEstimateId());
            } else {
                withoutBill.add(t.getServiceTicketId());
            }
        }
        Map<Integer, int[]> latest = new HashMap<>();
        queryIn("SELECT estimate_id, service_ticket_id, version FROM estimate WHERE service_ticket_id IN (:ids)",
                withoutBill, rs -> {
                    int ticketId = rs.getInt("service_ticket_id");
                    int estimateId = rs.getInt("estimate_id");
                    int version = rs.getInt("version");
                    int[] current = latest.get(ticketId);
                    if (current == null || version > current[1] || (version == current[1] && estimateId > current[0])) {
                        latest.put(ticketId, new int[]{estimateId, version});
                    }
                });
        latest.forEach((ticketId, v) -> estimateByTicket.put(ticketId, v[0]));

        Map<Integer, List<Line>> linesByEstimate = loadLines(estimateByTicket.values());
        Map<Integer, BigDecimal> commissionByEstimate = loadCommissions(estimateByTicket.values());
        loadIssues(tickets.keySet(), s.issues);
        Map<Integer, String> methodByBill = loadPaymentMethods(billByTicket.values().stream()
                .map(ServiceBillJpa::getBillId).filter(Objects::nonNull).toList());
        Map<Integer, Map<String, LinkedHashSet<String>>> crew = loadCrew(tickets.keySet());
        Map<Integer, String> staffNames = loadStaffNames(tickets.values().stream().flatMap(t -> Stream.of(
                t.getCreatedBy(), t.getBackfillAdvisorId(), t.getBackfillTechnicianId())));
        Map<Integer, Integer> odometer = loadOdometer(tickets.keySet());

        for (ServiceTicketJpa t : tickets.values()) {
            Integer id = t.getServiceTicketId();
            TicketCtx c = new TicketCtx(t, billByTicket.get(id));
            c.estimateId = estimateByTicket.get(id);
            List<Line> lines = c.estimateId != null ? linesByEstimate.getOrDefault(c.estimateId, List.of()) : List.of();
            // Hoá đơn chỉ gồm dòng khách đã duyệt; phiếu đang làm dở thì hiện cả dòng chưa duyệt.
            c.lines = c.bill != null ? lines.stream().filter(l -> l.checked).toList() : lines;
            Map<String, LinkedHashSet<String>> roles = crew.getOrDefault(id, Map.of());
            c.advisor = joinOr(roles.get("ADVISOR"), staffNames.get(t.getBackfillAdvisorId()));
            c.technician = joinOr(roles.get("TECHNICIAN"), staffNames.get(t.getBackfillTechnicianId()));
            c.createdBy = nz(staffNames.get(t.getCreatedBy()));
            c.odometer = odometer.get(id);
            if (c.bill != null) {
                String method = methodByBill.get(c.bill.getBillId());
                c.paymentMethod = method != null ? method : nz(t.getBackfillPaymentMethod());
            }
            compute(c, s.issues.getOrDefault(id, Map.of()),
                    c.estimateId != null ? commissionByEstimate.getOrDefault(c.estimateId, ZERO) : ZERO);
            s.tickets.add(c);
        }
        s.tickets.sort(Comparator.comparing((TicketCtx c) -> c.t.getReceivedAt(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(c -> nz(c.t.getTicketCode())));
        return s;
    }

    /** Tiền của một phiếu: doanh thu, VAT, phân bổ xuống dòng, giá vốn, lợi nhuận. */
    private static void compute(TicketCtx c, Map<Integer, IssueAgg> issues, BigDecimal commission) {
        c.cancelled = !RevenueReportService.isCountable(c.t);
        PaymentStatus status = c.bill != null ? c.bill.getPaymentStatus() : null;
        if (status == PaymentStatus.PAID && c.bill.getPaidAt() != null) {
            c.paidAt = LocalDateTime.ofInstant(c.bill.getPaidAt(), ZONE);
        }
        c.counted = !c.cancelled && c.bill != null
                && status != PaymentStatus.REFUNDED && status != PaymentStatus.VOID;
        c.paid = c.counted && status == PaymentStatus.PAID;
        if (!c.counted) {
            return;
        }

        c.revenue = nvl(c.bill.getFinalAmount());
        BigDecimal finalSum = ZERO;
        BigDecimal netSum = ZERO;
        for (Line l : c.lines) {
            finalSum = finalSum.add(l.finalPrice);
            netSum = netSum.add(l.finalPrice.subtract(l.tax));
        }
        c.allocatable = finalSum.signum() > 0;
        c.netRevenue = c.allocatable
                ? c.revenue.multiply(netSum).divide(finalSum, 0, RoundingMode.HALF_UP)
                : c.revenue;
        c.vat = c.revenue.subtract(c.netRevenue);

        if (c.allocatable) {
            int last = -1;
            for (int i = 0; i < c.lines.size(); i++) {
                if (c.lines.get(i).finalPrice.signum() != 0) last = i;
            }
            BigDecimal restGross = c.revenue;
            BigDecimal restNet = c.netRevenue;
            for (int i = 0; i < c.lines.size(); i++) {
                Line l = c.lines.get(i);
                if (l.finalPrice.signum() == 0) continue;
                BigDecimal gross;
                BigDecimal net;
                if (i == last) {
                    gross = restGross;
                    net = restNet;
                } else {
                    gross = c.revenue.multiply(l.finalPrice).divide(finalSum, 0, RoundingMode.HALF_UP);
                    net = gross.multiply(l.finalPrice.subtract(l.tax)).divide(l.finalPrice, 0, RoundingMode.HALF_UP);
                }
                l.allocated = gross;
                l.allocatedNet = net;
                restGross = restGross.subtract(gross);
                restNet = restNet.subtract(net);
            }
        }

        Map<Integer, BigDecimal> stockQtyByItem = new HashMap<>();
        for (Line l : c.lines) {
            if (l.outsource) {
                c.outsourceCost = c.outsourceCost.add(l.laborCost);
            } else if (l.itemId != null && "PART".equals(l.kind) && !l.tradeIn) {
                stockQtyByItem.merge(l.itemId, l.qty, BigDecimal::add);
            }
        }
        boolean incomplete = false;
        for (IssueAgg ia : issues.values()) {
            c.goodsCost = c.goodsCost.add(ia.cost);
            if (ia.noLotQty.signum() > 0) incomplete = true;
        }
        for (Map.Entry<Integer, BigDecimal> e : stockQtyByItem.entrySet()) {
            IssueAgg ia = issues.get(e.getKey());
            if (ia == null || ia.qty.compareTo(e.getValue()) < 0) incomplete = true;
        }
        c.costIncomplete = incomplete;
        c.commission = nvl(commission);
        c.profit = c.netRevenue.subtract(c.goodsCost).subtract(c.outsourceCost).subtract(c.commission);
    }

    private static ServiceBillJpa pickBill(ServiceBillJpa current, ServiceBillJpa candidate) {
        return RevenueReportService.preferBill(candidate, current) ? candidate : current;
    }

    private List<LegacyVisitJpa> loadLegacy(DataExportOptions o) {
        // Sổ cũ không ghi xưởng — toàn bộ là của xưởng mặc định (xưởng có trước khi có nhiều xưởng).
        if (o.branchId() != null && !o.branchId().equals(branchDirectory.defaultBranchId())) {
            return List.of();
        }
        return legacyVisitRepository.findByVisitedAtBetweenOrderByVisitedAtAsc(
                o.from().atStartOfDay(), o.to().atTime(LocalTime.MAX));
    }

    private void loadPeople(Scope s, List<LegacyVisitJpa> legacy) {
        Set<Integer> customerIds = new HashSet<>();
        Set<Integer> vehicleIds = new HashSet<>();
        s.tickets.forEach(c -> {
            customerIds.add(c.t.getCustomerId());
            vehicleIds.add(c.t.getVehicleId());
        });
        legacy.forEach(v -> {
            customerIds.add(v.getCustomerId());
            vehicleIds.add(v.getVehicleId());
        });
        queryIn("SELECT customer_id, full_name, phone, customer_code FROM customer_profile WHERE customer_id IN (:ids)",
                customerIds, rs -> s.customers.put(rs.getInt("customer_id"),
                        new Person(nz(rs.getString("full_name")), nz(rs.getString("phone")), nz(rs.getString("customer_code")))));
        queryIn("SELECT vehicle_id, license_plate, brand, model FROM vehicle WHERE vehicle_id IN (:ids)",
                vehicleIds, rs -> s.vehicles.put(rs.getInt("vehicle_id"),
                        new Car(nz(rs.getString("license_plate")), nz(rs.getString("brand")), nz(rs.getString("model")))));
    }

    private Map<Integer, List<Line>> loadLines(Collection<Integer> estimateIds) {
        Map<Integer, List<Line>> result = new HashMap<>();
        queryIn("""
                SELECT ei.estimate_item_id, ei.estimate_id, ei.item_id, ei.item_name, ei.quantity, ei.unit,
                       ei.unit_price, ei.total_price, ei.discount_amount, ei.tax_amount, ei.final_price,
                       ei.is_gift, ei.is_outsource, ei.labor_cost, ei.line_type, ei.category_label, ei.is_checked,
                       ci.sku, ci.item_name AS catalog_name, ci.item_type, ci.unit AS catalog_unit,
                       ic.category_name, ic.category_type
                FROM estimate_item ei
                LEFT JOIN catalog_item ci ON ci.item_id = ei.item_id
                LEFT JOIN item_category ic
                       ON ic.item_category_id = COALESCE(ei.item_category_id, ci.item_category_id)
                WHERE ei.estimate_id IN (:ids) AND COALESCE(ei.is_removed, 0) = 0
                ORDER BY ei.estimate_id, ei.estimate_item_id
                """, estimateIds, rs -> {
            Line l = new Line();
            l.itemId = rs.getObject("item_id", Integer.class);
            String catalogName = nz(rs.getString("catalog_name")).trim();
            String lineName = nz(rs.getString("item_name")).trim();
            l.name = !lineName.isEmpty() ? lineName : catalogName;
            l.productName = l.itemId != null && !catalogName.isEmpty() ? catalogName : l.name;
            l.sku = nz(rs.getString("sku"));
            l.kind = kindOf(rs.getString("item_type"), rs.getString("category_type"));
            String categoryName = nz(rs.getString("category_name")).trim();
            String label = nz(rs.getString("category_label")).trim();
            l.category = !categoryName.isEmpty() ? categoryName : (!label.isEmpty() ? label : NO_CATEGORY);
            String unit = nz(rs.getString("unit")).trim();
            l.unit = !unit.isEmpty() ? unit : nz(rs.getString("catalog_unit"));
            l.qty = nvl(rs.getBigDecimal("quantity"));
            l.unitPrice = nvl(rs.getBigDecimal("unit_price"));
            BigDecimal total = rs.getBigDecimal("total_price");
            l.totalPrice = total != null ? total : l.unitPrice.multiply(l.qty);
            l.discount = nvl(rs.getBigDecimal("discount_amount"));
            l.tax = nvl(rs.getBigDecimal("tax_amount"));
            BigDecimal fin = rs.getBigDecimal("final_price");
            l.finalPrice = fin != null ? fin : l.totalPrice.subtract(l.discount).add(l.tax);
            l.gift = rs.getBoolean("is_gift");
            l.outsource = rs.getBoolean("is_outsource");
            l.laborCost = nvl(rs.getBigDecimal("labor_cost"));
            l.tradeIn = "TRADE_IN".equalsIgnoreCase(nz(rs.getString("line_type")));
            l.checked = rs.getBoolean("is_checked");
            result.computeIfAbsent(rs.getInt("estimate_id"), k -> new ArrayList<>()).add(l);
        });
        return result;
    }

    private Map<Integer, BigDecimal> loadCommissions(Collection<Integer> estimateIds) {
        Map<Integer, BigDecimal> result = new HashMap<>();
        queryIn("""
                SELECT estimate_id, SUM(COALESCE(amount, 0)) AS amount
                FROM commission_allocation WHERE estimate_id IN (:ids) GROUP BY estimate_id
                """, estimateIds, rs -> result.put(rs.getInt("estimate_id"), nvl(rs.getBigDecimal("amount"))));
        return result;
    }

    /**
     * Giá vốn thực tế theo lô của hàng đã xuất cho phiếu. Dòng {@code entry_item_id = 0} là dòng
     * xuất vượt tồn không có lô (giá nhập = 0) — đếm riêng để báo giá vốn chưa đủ.
     */
    private void loadIssues(Collection<Integer> ticketIds, Map<Integer, Map<Integer, IssueAgg>> into) {
        queryIn("""
                SELECT s.service_ticket_id, i.item_id,
                       SUM(i.quantity) AS qty,
                       SUM(i.import_price * i.quantity) AS cost,
                       SUM(CASE WHEN i.entry_item_id IS NULL OR i.entry_item_id = 0 THEN i.quantity ELSE 0 END) AS no_lot_qty,
                       MAX(ci.item_name) AS item_name, MAX(ci.sku) AS sku, MAX(ci.item_type) AS item_type,
                       MAX(ci.unit) AS unit
                FROM stock_issue_item i
                JOIN stock_issue s ON s.issue_id = i.issue_id
                LEFT JOIN catalog_item ci ON ci.item_id = i.item_id
                WHERE s.status = 'CONFIRMED' AND s.service_ticket_id IN (:ids)
                GROUP BY s.service_ticket_id, i.item_id
                """, ticketIds, rs -> into.computeIfAbsent(rs.getInt("service_ticket_id"), k -> new HashMap<>())
                .put(rs.getInt("item_id"), new IssueAgg(
                        nvl(rs.getBigDecimal("qty")), nvl(rs.getBigDecimal("cost")), nvl(rs.getBigDecimal("no_lot_qty")),
                        nz(rs.getString("item_name")), nz(rs.getString("sku")), nz(rs.getString("item_type")),
                        nz(rs.getString("unit")))));
    }

    private Map<Integer, String> loadPaymentMethods(Collection<Integer> billIds) {
        Map<Integer, String> result = new HashMap<>();
        queryIn("""
                SELECT bill_id, method FROM payment_transaction
                WHERE bill_id IN (:ids) AND (status IS NULL OR status = 'SUCCESS')
                ORDER BY paid_at, transaction_id
                """, billIds, rs -> {
            String method = nz(rs.getString("method")).trim();
            if (!method.isEmpty()) result.put(rs.getInt("bill_id"), method.toUpperCase());
        });
        return result;
    }

    private Map<Integer, Map<String, LinkedHashSet<String>>> loadCrew(Collection<Integer> ticketIds) {
        Map<Integer, Map<String, LinkedHashSet<String>>> result = new HashMap<>();
        queryIn("""
                SELECT a.service_ticket_id, a.role_in_ticket, s.full_name
                FROM service_ticket_assignment a
                JOIN staff_profile s ON s.staff_id = a.staff_id
                WHERE a.service_ticket_id IN (:ids) AND a.role_in_ticket IN ('ADVISOR', 'TECHNICIAN')
                ORDER BY COALESCE(a.is_primary, 0) DESC, a.assignment_id
                """, ticketIds, rs -> {
            String name = nz(rs.getString("full_name")).trim();
            if (name.isEmpty()) return;
            result.computeIfAbsent(rs.getInt("service_ticket_id"), k -> new HashMap<>())
                    .computeIfAbsent(nz(rs.getString("role_in_ticket")), k -> new LinkedHashSet<>())
                    .add(name);
        });
        return result;
    }

    private Map<Integer, String> loadStaffNames(Stream<Integer> ids) {
        Map<Integer, String> result = new HashMap<>();
        queryIn("SELECT staff_id, full_name FROM staff_profile WHERE staff_id IN (:ids)",
                ids.filter(Objects::nonNull).collect(Collectors.toSet()),
                rs -> result.put(rs.getInt("staff_id"), nz(rs.getString("full_name"))));
        return result;
    }

    private Map<Integer, Integer> loadOdometer(Collection<Integer> ticketIds) {
        Map<Integer, Integer> result = new HashMap<>();
        queryIn("""
                SELECT service_ticket_id, MAX(reading) AS reading FROM odometer_history
                WHERE service_ticket_id IN (:ids) GROUP BY service_ticket_id
                """, ticketIds, rs -> result.put(rs.getInt("service_ticket_id"), rs.getObject("reading", Integer.class)));
        return result;
    }

    // ==========================================================================================
    // Tab: Khách hàng + Xe của khách

    private void writeCustomers(XlsxWriter x, DataExportOptions o, Scope s, List<LegacyVisitJpa> legacy,
                                Map<String, Integer> sheetRows, Overview ov) {
        List<CustomerRow> customers = new ArrayList<>();
        each("""
                SELECT cp.customer_id, cp.customer_code, cp.full_name, cp.phone, cp.email, cp.dob, cp.gender,
                       cp.customer_type, cp.is_company, cp.company_name, cp.tax_code, cp.address, cp.ward_name,
                       cp.district_name, cp.province_name, cp.identity_card, cg.name AS group_name,
                       cp.notification_channel, cp.do_not_contact, cp.contact_name, cp.contact_phone,
                       cp.note, cp.created_at, pts.current_rank, pts.total_points
                FROM customer_profile cp
                LEFT JOIN customer_group cg ON cg.group_id = cp.customer_group_id
                LEFT JOIN customer_points pts ON pts.customer_id = cp.customer_id
                WHERE NOT EXISTS (SELECT 1 FROM customer_auth a
                                  WHERE a.customer_id = cp.customer_id AND a.status = 'DELETED')
                  AND (cp.customer_type IS NULL OR cp.customer_type NOT IN ('ASSESSOR', 'BROKER', 'VENDOR'))
                ORDER BY cp.customer_id
                """, new MapSqlParameterSource(), rs -> customers.add(CustomerRow.of(rs)));

        Map<Integer, List<String>> otherPhones = new HashMap<>();
        each("SELECT customer_id, phone FROM customer_phone ORDER BY customer_phone_id", new MapSqlParameterSource(),
                rs -> otherPhones.computeIfAbsent(rs.getInt("customer_id"), k -> new ArrayList<>())
                        .add(nz(rs.getString("phone")).trim()));

        Map<Integer, List<VehicleRow>> vehiclesByCustomer = new HashMap<>();
        each("""
                SELECT vehicle_id, customer_id, license_plate, brand, model, manufacture_year
                FROM vehicle WHERE customer_id IS NOT NULL ORDER BY vehicle_id
                """, new MapSqlParameterSource(), rs -> vehiclesByCustomer
                .computeIfAbsent(rs.getInt("customer_id"), k -> new ArrayList<>())
                .add(new VehicleRow(rs.getInt("vehicle_id"), nz(rs.getString("license_plate")),
                        nz(rs.getString("brand")), nz(rs.getString("model")),
                        rs.getObject("manufacture_year", Integer.class))));

        Map<Integer, Visits> lifetimeByCustomer = loadVisitStats("customer_id");
        Map<Integer, Visits> lifetimeByVehicle = loadVisitStats("vehicle_id");
        Map<Integer, BigDecimal> lifetimePaid = new HashMap<>();
        each("""
                SELECT t.customer_id, SUM(t.amount) AS amount FROM (
                    SELECT st.customer_id, st.service_ticket_id, MAX(b.final_amount) AS amount
                    FROM service_bill b
                    JOIN service_ticket st ON st.service_ticket_id = b.service_ticket_id
                    WHERE b.payment_status = 'PAID'
                      AND COALESCE(st.is_deleted, 0) = 0
                      AND (st.ticket_status IS NULL OR st.ticket_status <> 'CANCELLED')
                      AND (st.backfill_review_status IS NULL OR st.backfill_review_status <> 'REJECTED')
                    GROUP BY st.customer_id, st.service_ticket_id
                ) t GROUP BY t.customer_id
                """, new MapSqlParameterSource(),
                rs -> lifetimePaid.put(rs.getInt("customer_id"), nvl(rs.getBigDecimal("amount"))));

        Map<Integer, Visits> legacyLifetime = new HashMap<>();
        if (o.includeLegacy()) {
            each("""
                    SELECT customer_id, COUNT(*) AS cnt, MAX(visited_at) AS last_at,
                           SUM(COALESCE(total_amount, 0)) AS amount
                    FROM legacy_visit GROUP BY customer_id
                    """, new MapSqlParameterSource(), rs -> legacyLifetime.put(rs.getInt("customer_id"),
                    new Visits(rs.getLong("cnt"), rs.getObject("last_at", LocalDateTime.class),
                            nvl(rs.getBigDecimal("amount")))));
        }

        // Trong kỳ (đã lọc xưởng): phiếu không huỷ + doanh thu hoá đơn; lượt sổ cũ tách riêng.
        Map<Integer, long[]> rangeCount = new HashMap<>();
        Map<Integer, BigDecimal> rangeRevenue = new HashMap<>();
        for (TicketCtx c : s.tickets) {
            if (c.cancelled || c.t.getCustomerId() == null) continue;
            rangeCount.computeIfAbsent(c.t.getCustomerId(), k -> new long[1])[0]++;
            rangeRevenue.merge(c.t.getCustomerId(), c.revenue, BigDecimal::add);
        }
        Map<Integer, long[]> rangeLegacyCount = new HashMap<>();
        Map<Integer, BigDecimal> rangeLegacyAmount = new HashMap<>();
        for (LegacyVisitJpa v : legacy) {
            if (v.getCustomerId() == null) continue;
            rangeLegacyCount.computeIfAbsent(v.getCustomerId(), k -> new long[1])[0]++;
            rangeLegacyAmount.merge(v.getCustomerId(), nvl(v.getTotalAmount()), BigDecimal::add);
        }

        List<Col> cols = new ArrayList<>(List.of(
                Col.integer("STT"), Col.text("Mã KH", 14), Col.text("Họ tên", 28), Col.text("SĐT chính", 14),
                Col.text("SĐT khác", 22), Col.text("Email", 28), Col.text("Loại khách", 12),
                Col.text("Tên công ty", 26), Col.text("Mã số thuế", 14), Col.text("Giới tính", 9),
                Col.date("Ngày sinh"), Col.wrap("Địa chỉ", 40), Col.text("CCCD / CMND", 15),
                Col.text("Nhóm khách", 16), Col.text("Hạng", 10), Col.integer("Điểm tích luỹ"),
                Col.text("Kênh nhận thông báo", 12), Col.text("Không liên hệ", 10),
                Col.text("Người liên hệ", 20), Col.text("SĐT liên hệ", 14), Col.wrap("Biển số xe", 22),
                Col.integer("Số xe"), Col.dateTime("Ngày tạo hồ sơ"), Col.integer("Phiếu trong kỳ"),
                Col.money("Doanh thu trong kỳ"), Col.integer("Tổng số phiếu"),
                Col.money("Tổng đã thanh toán"), Col.dateTime("Lần cuối đến xưởng")));
        if (o.includeLegacy()) {
            cols.addAll(List.of(Col.integer("Lượt sổ cũ trong kỳ"), Col.money("Tiền sổ cũ trong kỳ"),
                    Col.integer("Tổng lượt sổ cũ")));
        }
        cols.add(Col.wrap("Ghi chú", 36));

        XlsxWriter.Table sheet = x.table("Khách hàng", cols);
        List<CustomerRow> exported = new ArrayList<>();
        int stt = 1;
        for (CustomerRow c : customers) {
            long inRange = rangeCount.getOrDefault(c.id, new long[1])[0];
            long legacyInRange = rangeLegacyCount.getOrDefault(c.id, new long[1])[0];
            if (o.activeCustomersOnly() && inRange == 0 && legacyInRange == 0) continue;
            exported.add(c);

            List<VehicleRow> vehicles = vehiclesByCustomer.getOrDefault(c.id, List.of());
            List<String> phones = otherPhones.getOrDefault(c.id, List.of()).stream()
                    .filter(p -> !p.isEmpty() && !p.equals(c.phone)).distinct().toList();
            Visits life = lifetimeByCustomer.get(c.id);
            Visits legacyLife = legacyLifetime.get(c.id);
            LocalDateTime lastVisit = latestOf(life != null ? life.last : null, legacyLife != null ? legacyLife.last : null);

            List<Object> values = new ArrayList<>(List.of(
                    stt++, c.code, c.name, c.phone, String.join(", ", phones), c.email, customerTypeLabel(c.type),
                    c.companyName, c.taxCode, genderLabel(c.gender)));
            values.add(c.dob);
            values.addAll(List.of(c.address, c.identityCard, c.groupName, rankLabel(c.rank)));
            values.add(c.points);
            values.addAll(List.of(c.channel, c.doNotContact ? "Có" : "", c.contactName, c.contactPhone,
                    vehicles.stream().map(VehicleRow::plate).filter(p -> !p.isBlank()).collect(Collectors.joining(", "))));
            values.add(vehicles.size());
            values.add(c.createdAt);
            values.add(inRange);
            values.add(rangeRevenue.getOrDefault(c.id, ZERO));
            values.add(life != null ? life.count : 0);
            values.add(lifetimePaid.getOrDefault(c.id, ZERO));
            values.add(lastVisit);
            if (o.includeLegacy()) {
                values.add(legacyInRange);
                values.add(rangeLegacyAmount.getOrDefault(c.id, ZERO));
                values.add(legacyLife != null ? legacyLife.count : 0);
            }
            values.add(c.note);
            sheet.row(values.toArray());
        }
        sheet.finish();
        sheetRows.put("Khách hàng", sheet.rowCount());
        ov.customers = sheet.rowCount();
        ov.activeCustomers = exported.stream()
                .filter(c -> rangeCount.containsKey(c.id) || rangeLegacyCount.containsKey(c.id)).count();

        XlsxWriter.Table cars = x.table("Xe của khách", List.of(
                Col.integer("STT"), Col.text("Biển số", 14), Col.text("Hãng xe", 16), Col.text("Dòng xe", 20),
                Col.integer("Năm sản xuất"), Col.text("Mã KH", 14), Col.text("Chủ xe", 28),
                Col.text("SĐT chủ xe", 14), Col.integer("Số phiếu"), Col.dateTime("Lần cuối vào xưởng")));
        int carNo = 1;
        for (CustomerRow c : exported) {
            for (VehicleRow v : vehiclesByCustomer.getOrDefault(c.id, List.of())) {
                Visits visits = lifetimeByVehicle.get(v.id);
                cars.row(carNo++, v.plate, v.brand, v.model, v.year, c.code, c.name, c.phone,
                        visits != null ? visits.count : 0, visits != null ? visits.last : null);
            }
        }
        cars.finish();
        sheetRows.put("Xe của khách", cars.rowCount());
        ov.vehicles = cars.rowCount();
    }

    /** Số phiếu (không huỷ, không xoá) và lần tiếp nhận gần nhất, nhóm theo cột đã cho. */
    private Map<Integer, Visits> loadVisitStats(String column) {
        Map<Integer, Visits> result = new HashMap<>();
        // column chỉ nhận hằng số trong code (customer_id / vehicle_id), không bao giờ từ request.
        each("SELECT " + column + " AS k, COUNT(*) AS cnt, MAX(received_at) AS last_at FROM service_ticket"
                        + " WHERE " + column + " IS NOT NULL AND COALESCE(is_deleted, 0) = 0"
                        + " AND (ticket_status IS NULL OR ticket_status <> 'CANCELLED')"
                        + " GROUP BY " + column,
                new MapSqlParameterSource(), rs -> result.put(rs.getInt("k"),
                        new Visits(rs.getLong("cnt"), rs.getObject("last_at", LocalDateTime.class), ZERO)));
        return result;
    }

    // ==========================================================================================
    // Tab: Doanh thu sản phẩm / Biên LN theo sản phẩm

    private List<ProductAgg> aggregateProducts(Scope s) {
        Map<String, ProductAgg> map = new LinkedHashMap<>();
        for (TicketCtx c : s.tickets) {
            if (!c.counted) continue;
            Integer ticketId = c.t.getServiceTicketId();
            Map<Integer, IssueAgg> issues = s.issues.getOrDefault(ticketId, Map.of());
            Set<Integer> costed = new HashSet<>();
            for (Line l : c.lines) {
                String key = l.itemId != null ? "i:" + l.itemId
                        : "n:" + l.name.toLowerCase() + "|" + l.unit.toLowerCase();
                ProductAgg p = map.computeIfAbsent(key,
                        k -> new ProductAgg(l.sku, l.productName, l.kind, l.category, l.unit));
                p.tickets.add(ticketId);
                if (l.gift) {
                    p.giftQty = p.giftQty.add(l.qty);
                } else {
                    p.qty = p.qty.add(l.qty);
                }
                p.gross = p.gross.add(l.totalPrice);
                p.discount = p.discount.add(l.discount);
                p.tax = p.tax.add(l.tax);
                p.lineTotal = p.lineTotal.add(l.finalPrice);
                p.revenue = p.revenue.add(l.allocated);
                if (c.paid) p.paidRevenue = p.paidRevenue.add(l.allocated);
                p.netRevenue = p.netRevenue.add(l.allocatedNet);
                if (l.outsource) p.outsource = p.outsource.add(l.laborCost);
                if (l.itemId != null && costed.add(l.itemId)) {
                    IssueAgg ia = issues.get(l.itemId);
                    if (ia != null) {
                        p.cost = p.cost.add(ia.cost);
                        p.issuedQty = p.issuedQty.add(ia.qty);
                        if (ia.noLotQty.signum() > 0) p.costIncomplete = true;
                    } else if ("PART".equals(l.kind) && !l.outsource && !l.tradeIn) {
                        p.costIncomplete = true;
                    }
                }
            }
            // Hàng đã xuất kho cho phiếu nhưng không còn dòng báo giá nào trỏ tới.
            for (Map.Entry<Integer, IssueAgg> e : issues.entrySet()) {
                if (costed.contains(e.getKey())) continue;
                IssueAgg ia = e.getValue();
                ProductAgg p = map.computeIfAbsent("i:" + e.getKey(),
                        k -> new ProductAgg(ia.sku, ia.name, kindOf(ia.itemType, null), NO_CATEGORY, ia.unit));
                p.tickets.add(ticketId);
                p.cost = p.cost.add(ia.cost);
                p.issuedQty = p.issuedQty.add(ia.qty);
            }
            if (!c.allocatable && c.revenue.signum() != 0) {
                ProductAgg p = map.computeIfAbsent("unallocated",
                        k -> new ProductAgg("", "(Hoá đơn không có dòng báo giá để chia)", "OTHER", NO_CATEGORY, ""));
                p.tickets.add(ticketId);
                p.revenue = p.revenue.add(c.revenue);
                if (c.paid) p.paidRevenue = p.paidRevenue.add(c.revenue);
                p.netRevenue = p.netRevenue.add(c.netRevenue);
            }
        }
        return new ArrayList<>(map.values());
    }

    private void writeProductRevenue(XlsxWriter x, List<ProductAgg> products, Map<String, Integer> sheetRows) {
        BigDecimal total = products.stream().map(p -> p.revenue).reduce(ZERO, BigDecimal::add);
        XlsxWriter.Table sheet = x.table("Doanh thu sản phẩm", List.of(
                Col.integer("STT"), Col.text("SKU", 14), Col.wrap("Tên sản phẩm / dịch vụ", 40),
                Col.text("Loại", 14), Col.text("Danh mục", 20), Col.text("ĐVT", 8), Col.integer("Số phiếu"),
                Col.qty("Số lượng bán"), Col.qty("Số lượng tặng"), Col.money("Đơn giá bình quân"),
                Col.money("Thành tiền trước giảm"), Col.money("Giảm giá trên dòng"), Col.money("Thuế VAT"),
                Col.money("Thành tiền theo báo giá"), Col.money("Doanh thu (phân bổ theo hoá đơn)"),
                Col.money("Trong đó đã thu"), Col.percent("Tỷ trọng doanh thu")));
        int stt = 1;
        for (ProductAgg p : sorted(products, Comparator.comparing((ProductAgg a) -> a.revenue).reversed())) {
            sheet.row(stt++, p.sku, p.name, kindLabel(p.kind), p.category, p.unit, p.tickets.size(),
                    p.qty, p.giftQty, p.qty.signum() > 0 ? p.lineTotal.divide(p.qty, 0, RoundingMode.HALF_UP) : null,
                    p.gross, p.discount, p.tax, p.lineTotal, p.revenue, p.paidRevenue, ratio(p.revenue, total));
        }
        sheet.finish();
        sheetRows.put("Doanh thu sản phẩm", sheet.rowCount());
    }

    private void writeProfitByProduct(XlsxWriter x, List<ProductAgg> products, Map<String, Integer> sheetRows) {
        XlsxWriter.Table sheet = x.table("Biên LN theo sản phẩm", List.of(
                Col.integer("STT"), Col.text("SKU", 14), Col.wrap("Tên sản phẩm / dịch vụ", 40),
                Col.text("Loại", 14), Col.text("Danh mục", 20), Col.text("ĐVT", 8), Col.qty("Số lượng bán"),
                Col.qty("Số lượng đã xuất kho"), Col.money("Doanh thu chưa VAT"), Col.money("Giá vốn hàng xuất kho"),
                Col.money("Tiền công thuê ngoài"), Col.money("Lợi nhuận gộp"), Col.percent("Biên lợi nhuận"),
                Col.wrap("Ghi chú", 40)));
        int stt = 1;
        for (ProductAgg p : sorted(products, Comparator.comparing(ProductAgg::profit).reversed())) {
            sheet.row(stt++, p.sku, p.name, kindLabel(p.kind), p.category, p.unit, p.qty.add(p.giftQty),
                    p.issuedQty, p.netRevenue, p.cost, p.outsource, p.profit(), ratio(p.profit(), p.netRevenue),
                    p.costIncomplete ? "Có lượt bán chưa xuất kho / xuất không có lô — giá vốn chưa đủ, lợi nhuận đang cao hơn thực tế" : "");
        }
        sheet.finish();
        sheetRows.put("Biên LN theo sản phẩm", sheet.rowCount());
    }

    // ==========================================================================================
    // Tab: Phiếu dịch vụ / Chi tiết hạng mục / Sổ dịch vụ cũ / Biên LN theo phiếu

    private void writeTickets(XlsxWriter x, Scope s, Map<String, Integer> sheetRows) {
        XlsxWriter.Table sheet = x.table("Phiếu dịch vụ", List.of(
                Col.integer("STT"), Col.text("Mã phiếu", 16), Col.text("Loại phiếu", 13), Col.text("Xưởng", 16),
                Col.dateTime("Ngày tiếp nhận"), Col.dateTime("Hẹn trả xe"), Col.dateTime("Ngày hoàn thành"),
                Col.dateTime("Ngày giao xe"), Col.text("Trạng thái", 15), Col.text("Nhập bù", 14),
                Col.text("Mã KH", 14), Col.text("Khách hàng", 26), Col.text("SĐT", 14), Col.text("Biển số", 13),
                Col.text("Hãng xe", 13), Col.text("Dòng xe", 16), Col.integer("Số km"), Col.text("Cố vấn", 20),
                Col.text("Kỹ thuật viên", 22), Col.text("Người tạo phiếu", 18), Col.wrap("Yêu cầu của khách", 36),
                Col.integer("Số hạng mục"), Col.money("Tiền hàng trước giảm"), Col.money("Giảm giá"),
                Col.money("Thành tiền"), Col.text("Thanh toán", 15), Col.dateTime("Ngày thu tiền"),
                Col.text("Phương thức", 13), Col.wrap("Lý do huỷ", 26)));
        int stt = 1;
        for (TicketCtx c : s.tickets) {
            ServiceTicketJpa t = c.t;
            Person person = person(s, c);
            Car car = t.getVehicleId() != null ? s.vehicles.get(t.getVehicleId()) : null;
            boolean money = c.bill != null && !c.cancelled;
            BigDecimal total = money ? nvl(c.bill.getFinalAmount()) : null;
            BigDecimal discount = money ? nvl(c.bill.getDiscountAmount()) : null;
            BigDecimal subtotal = money
                    ? (c.bill.getSubTotal() != null ? c.bill.getSubTotal() : total.add(discount)) : null;
            sheet.row(stt++, nz(t.getTicketCode()), ticketTypeLabel(t.getTicketType()),
                    branchDirectory.nameOf(t.getBranchId()), t.getReceivedAt(), t.getEstimatedDeliveryAt(),
                    t.getCompletedAt(), t.getDeliveredAt(), ticketStatusLabel(t), backfillLabel(t),
                    person.code, person.name, person.phone, car != null ? car.plate : "",
                    car != null ? car.brand : "", car != null ? car.model : "", c.odometer, c.advisor,
                    c.technician, c.createdBy, nz(t.getCustomerRequest()), c.lines.size(), subtotal, discount,
                    total, paymentLabel(c), c.paidAt, paymentMethodLabel(c.paymentMethod),
                    nz(t.getCancelReason()));
        }
        sheet.finish();
        sheetRows.put("Phiếu dịch vụ", sheet.rowCount());
    }

    private void writeTicketLines(XlsxWriter x, Scope s, Map<String, Integer> sheetRows) {
        XlsxWriter.Table sheet = x.table("Chi tiết hạng mục", List.of(
                Col.integer("STT"), Col.text("Mã phiếu", 16), Col.dateTime("Ngày tiếp nhận"),
                Col.text("Trạng thái phiếu", 15), Col.text("Thanh toán", 15), Col.text("Khách hàng", 26),
                Col.text("Biển số", 13), Col.text("Danh mục", 20), Col.text("SKU", 14),
                Col.wrap("Tên hạng mục", 40), Col.text("Loại", 14), Col.text("ĐVT", 8), Col.qty("Số lượng"),
                Col.money("Đơn giá"), Col.money("Thành tiền trước giảm"), Col.money("Giảm giá"),
                Col.money("Thuế VAT"), Col.money("Thành tiền"), Col.text("Hàng tặng", 9),
                Col.text("Thu mua lại", 10), Col.text("Thuê ngoài", 10), Col.money("Tiền công thuê ngoài"),
                Col.text("Khách đã duyệt", 11)));
        int stt = 1;
        for (TicketCtx c : s.tickets) {
            Person person = person(s, c);
            Car car = c.t.getVehicleId() != null ? s.vehicles.get(c.t.getVehicleId()) : null;
            String status = ticketStatusLabel(c.t);
            String payment = paymentLabel(c);
            for (Line l : c.lines) {
                sheet.row(stt++, nz(c.t.getTicketCode()), c.t.getReceivedAt(), status, payment, person.name,
                        car != null ? car.plate : "", l.category, l.sku, l.name, kindLabel(l.kind), l.unit,
                        l.qty, l.unitPrice, l.totalPrice, l.discount, l.tax, l.finalPrice,
                        l.gift ? "Có" : "", l.tradeIn ? "Có" : "", l.outsource ? "Có" : "",
                        l.outsource ? l.laborCost : null, l.checked ? "Có" : "Chưa");
            }
        }
        sheet.finish();
        sheetRows.put("Chi tiết hạng mục", sheet.rowCount());
    }

    private void writeLegacy(XlsxWriter x, Scope s, List<LegacyVisitJpa> legacy, Map<String, Integer> sheetRows) {
        XlsxWriter.Table sheet = x.table("Sổ dịch vụ cũ", List.of(
                Col.integer("STT"), Col.text("Mã sổ", 14), Col.dateTime("Ngày đến"), Col.dateTime("Ngày giao"),
                Col.text("Mã KH", 14), Col.text("Khách hàng", 26), Col.text("SĐT", 14), Col.text("Biển số", 13),
                Col.integer("Số km"), Col.wrap("Nội dung dịch vụ", 50), Col.wrap("Ghi chú của khách", 30),
                Col.money("Thành tiền"), Col.money("Giảm giá"), Col.text("Lệch tiền khi nhập", 12)));
        int stt = 1;
        for (LegacyVisitJpa v : legacy) {
            Person person = v.getCustomerId() != null ? s.customers.get(v.getCustomerId()) : null;
            Car car = v.getVehicleId() != null ? s.vehicles.get(v.getVehicleId()) : null;
            String code = nz(v.getLegacyTicketCode()).trim();
            sheet.row(stt++, !code.isEmpty() ? code : "SC-" + v.getLegacyVisitId(), v.getVisitedAt(),
                    v.getDeliveredAt(), person != null ? person.code : "", person != null ? person.name : "",
                    person != null ? person.phone : "", car != null ? car.plate : "", v.getOdometer(),
                    nz(v.getServicesText()), nz(v.getCustomerNote()), nvl(v.getTotalAmount()),
                    nvl(v.getDiscountAmount()), Boolean.TRUE.equals(v.getAmountMismatch()) ? "Có" : "");
        }
        sheet.finish();
        sheetRows.put("Sổ dịch vụ cũ", sheet.rowCount());
    }

    private void writeProfitByTicket(XlsxWriter x, Scope s, Map<String, Integer> sheetRows) {
        XlsxWriter.Table sheet = x.table("Biên LN theo phiếu", List.of(
                Col.integer("STT"), Col.text("Mã phiếu", 16), Col.text("Loại phiếu", 13), Col.text("Xưởng", 16),
                Col.dateTime("Ngày tiếp nhận"), Col.dateTime("Ngày thu tiền"), Col.text("Khách hàng", 26),
                Col.text("Biển số", 13), Col.text("Thanh toán", 13), Col.money("Doanh thu (gồm VAT)"),
                Col.money("Thuế VAT"), Col.money("Doanh thu chưa VAT"), Col.money("Giá vốn hàng xuất kho"),
                Col.money("Tiền công thuê ngoài"), Col.money("Hoa hồng & điều chỉnh"), Col.money("Lợi nhuận gộp"),
                Col.percent("Biên lợi nhuận"), Col.wrap("Ghi chú", 40)));
        int stt = 1;
        for (TicketCtx c : s.tickets) {
            if (!c.counted) continue;
            Person person = person(s, c);
            Car car = c.t.getVehicleId() != null ? s.vehicles.get(c.t.getVehicleId()) : null;
            List<String> notes = new ArrayList<>();
            if (c.costIncomplete) notes.add("Có hàng chưa xuất kho / xuất không có lô — giá vốn chưa đủ");
            if (!c.allocatable && c.revenue.signum() != 0) notes.add("Không có dòng báo giá — chưa tách được VAT");
            sheet.row(stt++, nz(c.t.getTicketCode()), ticketTypeLabel(c.t.getTicketType()),
                    branchDirectory.nameOf(c.t.getBranchId()), c.t.getReceivedAt(), c.paidAt, person.name,
                    car != null ? car.plate : "", paymentLabel(c), c.revenue, c.vat, c.netRevenue, c.goodsCost,
                    c.outsourceCost, c.commission, c.profit, ratio(c.profit, c.netRevenue), String.join("; ", notes));
        }
        sheet.finish();
        sheetRows.put("Biên LN theo phiếu", sheet.rowCount());
    }

    // ==========================================================================================
    // Tab: Tồn kho sản phẩm

    private void writeInventory(XlsxWriter x, DataExportOptions o, Map<String, Integer> sheetRows, Overview ov) {
        Map<Integer, ItemRow> items = new LinkedHashMap<>();
        each("""
                SELECT ci.item_id, ci.sku, ci.part_number, ci.item_name, ci.item_type, ci.unit, ci.price,
                       ic.category_name
                FROM catalog_item ci
                LEFT JOIN item_category ic ON ic.item_category_id = ci.item_category_id
                WHERE COALESCE(ci.is_active, 1) = 1
                ORDER BY ci.item_name, ci.item_id
                """, new MapSqlParameterSource(), rs -> {
            ItemRow item = new ItemRow();
            item.id = rs.getInt("item_id");
            item.sku = nz(rs.getString("sku"));
            item.partNumber = nz(rs.getString("part_number"));
            item.name = nz(rs.getString("item_name"));
            item.type = nz(rs.getString("item_type")).toUpperCase();
            item.unit = nz(rs.getString("unit"));
            item.price = rs.getBigDecimal("price");
            String category = nz(rs.getString("category_name")).trim();
            item.category = !category.isEmpty() ? category : NO_CATEGORY;
            items.put(item.id, item);
        });

        each("""
                SELECT inv.item_id, w.warehouse_name, w.warehouse_type, inv.quantity, inv.reserved_quantity,
                       inv.min_stock_level
                FROM inventory inv
                JOIN warehouse w ON w.warehouse_id = inv.warehouse_id
                ORDER BY w.warehouse_name
                """, new MapSqlParameterSource(), rs -> {
            ItemRow item = items.get(rs.getInt("item_id"));
            if (item == null) return;
            BigDecimal qty = nvl(rs.getBigDecimal("quantity"));
            String warehouse = nz(rs.getString("warehouse_name"));
            if ("DEFECTIVE".equalsIgnoreCase(nz(rs.getString("warehouse_type")))) {
                item.defectQty = item.defectQty.add(qty);
            } else {
                item.qty = item.qty.add(qty);
                item.reserved = item.reserved.add(nvl(rs.getBigDecimal("reserved_quantity")));
                item.minLevel += rs.getInt("min_stock_level");
            }
            item.hasInventory = true;
            if (qty.signum() != 0) item.byWarehouse.add(warehouse + ": " + plain(qty));
        });

        each("""
                SELECT sei.item_id, SUM(sei.remaining_quantity) AS qty,
                       SUM(sei.remaining_quantity * sei.import_price) AS value
                FROM stock_entry_item sei
                JOIN stock_entry se ON se.entry_id = sei.entry_id
                JOIN warehouse w ON w.warehouse_id = se.warehouse_id
                WHERE se.status = 'CONFIRMED' AND sei.remaining_quantity > 0
                  AND (w.warehouse_type IS NULL OR w.warehouse_type <> 'DEFECTIVE')
                GROUP BY sei.item_id
                """, new MapSqlParameterSource(), rs -> {
            ItemRow item = items.get(rs.getInt("item_id"));
            if (item == null) return;
            item.lotQty = nvl(rs.getBigDecimal("qty"));
            item.lotValue = nvl(rs.getBigDecimal("value"));
        });

        MapSqlParameterSource period = new MapSqlParameterSource()
                .addValue("fromDate", o.from())
                .addValue("toDate", o.to())
                .addValue("start", o.from().atStartOfDay())
                .addValue("end", o.to().atTime(LocalTime.MAX));
        each("""
                SELECT sei.item_id, SUM(sei.quantity) AS qty
                FROM stock_entry_item sei
                JOIN stock_entry se ON se.entry_id = sei.entry_id
                WHERE se.status = 'CONFIRMED' AND se.entry_date BETWEEN :fromDate AND :toDate
                GROUP BY sei.item_id
                """, period, rs -> {
            ItemRow item = items.get(rs.getInt("item_id"));
            if (item != null) item.inQty = nvl(rs.getBigDecimal("qty"));
        });
        each("""
                SELECT i.item_id, SUM(i.quantity) AS qty
                FROM stock_issue_item i
                JOIN stock_issue s ON s.issue_id = i.issue_id
                WHERE s.status = 'CONFIRMED' AND s.confirmed_at BETWEEN :start AND :end
                GROUP BY i.item_id
                """, period, rs -> {
            ItemRow item = items.get(rs.getInt("item_id"));
            if (item != null) item.outQty = nvl(rs.getBigDecimal("qty"));
        });

        XlsxWriter.Table sheet = x.table("Tồn kho sản phẩm", List.of(
                Col.integer("STT"), Col.text("SKU", 14), Col.text("Mã phụ tùng", 14),
                Col.wrap("Tên sản phẩm", 40), Col.text("Loại", 12), Col.text("Danh mục", 20), Col.text("ĐVT", 8),
                Col.qty("Tồn kho"), Col.qty("Đang giữ cho phiếu"), Col.qty("Khả dụng"), Col.qty("Tồn kho lỗi"),
                Col.wrap("Chi tiết theo kho", 30), Col.integer("Mức tồn tối thiểu"), Col.text("Tình trạng", 17),
                Col.money("Giá vốn bình quân"), Col.money("Giá trị tồn kho"), Col.money("Giá bán niêm yết"),
                Col.qty("Nhập trong kỳ"), Col.qty("Xuất trong kỳ"), Col.wrap("Ghi chú", 34)));
        int stt = 1;
        for (ItemRow item : items.values()) {
            if (!PHYSICAL_TYPES.contains(item.type) && !item.hasInventory) continue;
            String state;
            if (item.qty.signum() <= 0) {
                state = "Hết hàng";
                ov.outOfStock++;
            } else if (item.minLevel > 0 && item.qty.compareTo(BigDecimal.valueOf(item.minLevel)) < 0) {
                state = "Dưới mức tối thiểu";
                ov.belowMin++;
            } else {
                state = "Còn hàng";
            }
            BigDecimal avgCost = item.lotQty.signum() > 0
                    ? item.lotValue.divide(item.lotQty, 0, RoundingMode.HALF_UP) : null;
            String note = item.lotQty.compareTo(item.qty) != 0
                    ? "Tồn kho lệch tổng lô (" + plain(item.lotQty) + ") — giá trị tồn tính theo lô" : "";
            sheet.row(stt++, item.sku, item.partNumber, item.name, itemTypeLabel(item.type), item.category,
                    item.unit, item.qty, item.reserved, item.qty.subtract(item.reserved), item.defectQty,
                    String.join("; ", item.byWarehouse), item.minLevel, state, avgCost, item.lotValue, item.price,
                    item.inQty, item.outQty, note);
            ov.stockValue = ov.stockValue.add(item.lotValue);
        }
        sheet.finish();
        sheetRows.put("Tồn kho sản phẩm", sheet.rowCount());
        ov.items = sheet.rowCount();
    }

    // ==========================================================================================
    // Tab: Tổng quan

    private void writeOverview(XlsxWriter.KeyValue kv, DataExportOptions o, Actor actor, Scope s,
                               List<LegacyVisitJpa> legacy, Map<String, Integer> sheetRows, Overview ov) {
        kv.title("BÁO CÁO TỔNG HỢP");
        kv.pair("Kỳ báo cáo", o.from().format(DAY) + " – " + o.to().format(DAY), Fmt.TEXT);
        kv.pair("Chọn phiếu theo", o.basis().label(), Fmt.TEXT,
                o.basis() == DateBasis.PAID ? "Chỉ gồm phiếu có hoá đơn đã thu tiền trong kỳ"
                        : "Gồm cả phiếu chưa thu tiền / chưa lập hoá đơn");
        kv.pair("Xưởng", o.branchId() == null ? "Tất cả xưởng" : branchDirectory.nameOf(o.branchId()), Fmt.TEXT,
                "Tồn kho không lọc theo xưởng (kho dùng chung)");
        kv.pair("Dữ liệu xuất", o.types().stream().map(DataExportType::label).collect(Collectors.joining(", ")), Fmt.TEXT);
        kv.pair("Người xuất", actor.name(), Fmt.TEXT);
        kv.pair("Email đã xác minh", actor.maskedEmail(), Fmt.TEXT);
        kv.pair("Thời điểm xuất", LocalDateTime.now(), Fmt.DATETIME);

        if (s.loaded) {
            long total = s.tickets.size();
            long cancelled = s.tickets.stream().filter(c -> c.cancelled).count();
            long counted = s.tickets.stream().filter(c -> c.counted).count();
            long paid = s.tickets.stream().filter(c -> c.paid).count();
            long noBill = s.tickets.stream().filter(c -> !c.cancelled && c.bill == null).count();
            BigDecimal revenue = sumOf(s, c -> c.revenue);
            BigDecimal paidRevenue = s.tickets.stream().filter(c -> c.paid).map(c -> c.revenue)
                    .reduce(ZERO, BigDecimal::add);
            BigDecimal net = sumOf(s, c -> c.netRevenue);

            kv.section("Phiếu dịch vụ & doanh thu");
            kv.pair("Tổng số phiếu trong kỳ", total, Fmt.INT);
            kv.pair("Phiếu đã huỷ", cancelled, Fmt.INT, "Có trong tab phiếu nhưng không tính tiền");
            kv.pair("Phiếu chưa lập hoá đơn", noBill, Fmt.INT, "Đang làm dở — chưa phải doanh thu");
            kv.pair("Phiếu tính doanh thu", counted, Fmt.INT);
            kv.pair("Trong đó đã thu tiền", paid, Fmt.INT);
            kv.pair("Doanh thu (gồm VAT)", revenue, Fmt.MONEY);
            kv.pair("Đã thu", paidRevenue, Fmt.MONEY);
            kv.pair("Chưa thu", revenue.subtract(paidRevenue), Fmt.MONEY);
            kv.pair("Thuế VAT", revenue.subtract(net), Fmt.MONEY);
            kv.pair("Doanh thu chưa VAT", net, Fmt.MONEY);

            if (o.has(DataExportType.PROFIT)) {
                BigDecimal cost = sumOf(s, c -> c.goodsCost);
                BigDecimal outsource = sumOf(s, c -> c.outsourceCost);
                BigDecimal commission = sumOf(s, c -> c.commission);
                BigDecimal profit = sumOf(s, c -> c.profit);
                long incomplete = s.tickets.stream().filter(c -> c.counted && c.costIncomplete).count();
                kv.section("Biên lợi nhuận");
                kv.pair("Giá vốn hàng xuất kho", cost, Fmt.MONEY, "Theo giá nhập của lô đã xuất cho phiếu");
                kv.pair("Tiền công thuê ngoài", outsource, Fmt.MONEY);
                kv.pair("Hoa hồng & điều chỉnh", commission, Fmt.MONEY, "Khách, giám định viên, môi giới, điều chỉnh");
                kv.pair("Lợi nhuận gộp", profit, Fmt.MONEY);
                kv.pair("Biên lợi nhuận", ratio(profit, net), Fmt.PERCENT, "Lợi nhuận gộp / doanh thu chưa VAT");
                kv.pair("Phiếu giá vốn chưa đủ", incomplete, Fmt.INT,
                        incomplete > 0 ? "Có hàng chưa xuất kho hoặc xuất không có lô — lợi nhuận các phiếu này đang cao hơn thực tế" : "");
            }
        }

        if (o.has(DataExportType.CUSTOMERS)) {
            kv.section("Khách hàng");
            kv.pair("Số khách hàng xuất", ov.customers, Fmt.INT,
                    o.activeCustomersOnly() ? "Chỉ khách có phiếu / lượt sổ cũ trong kỳ" : "Toàn bộ danh bạ khách hàng (không gồm đối tác GĐV / môi giới / nhà cung cấp)");
            kv.pair("Khách có phát sinh trong kỳ", ov.activeCustomers, Fmt.INT);
            kv.pair("Số xe", ov.vehicles, Fmt.INT);
        }

        if (o.has(DataExportType.INVENTORY)) {
            kv.section("Tồn kho (tại thời điểm xuất)");
            kv.pair("Số mặt hàng", ov.items, Fmt.INT);
            kv.pair("Giá trị tồn kho (theo lô)", ov.stockValue, Fmt.MONEY, "Không gồm kho hàng lỗi");
            kv.pair("Mặt hàng hết hàng", ov.outOfStock, Fmt.INT);
            kv.pair("Mặt hàng dưới mức tối thiểu", ov.belowMin, Fmt.INT);
        }

        if (o.includeLegacy() && !legacy.isEmpty()) {
            kv.section("Sổ dịch vụ cũ (tách riêng)");
            kv.pair("Số lượt trong kỳ", legacy.size(), Fmt.INT);
            kv.pair("Tiền ghi trong sổ cũ", legacy.stream().map(v -> nvl(v.getTotalAmount()))
                    .reduce(ZERO, BigDecimal::add), Fmt.MONEY, "Số chép tay, KHÔNG cộng vào doanh thu hoá đơn ở trên");
        }

        kv.section("Các tab trong file");
        sheetRows.forEach((sheet, rows) -> kv.pair(sheet, rows, Fmt.INT, "dòng"));

        kv.section("Lưu ý");
        kv.note("File chứa thông tin cá nhân của khách hàng và số liệu tài chính nội bộ. Không gửi qua Zalo / email cá nhân, "
                + "không để trên máy dùng chung. Mỗi lần xuất đều được ghi vào nhật ký hệ thống.");
    }

    // ==========================================================================================
    // Nhãn tiếng Việt

    private static String paymentLabel(TicketCtx c) {
        if (c.cancelled) return "Đã huỷ";
        if (c.bill == null) return "Chưa lập hoá đơn";
        PaymentStatus status = c.bill.getPaymentStatus();
        if (status == null) return "Chưa thu";
        return switch (status) {
            case PAID -> "Đã thu";
            case PARTIALLY_PAID -> "Thu một phần";
            case REFUNDED -> "Đã hoàn tiền";
            case VOID -> "Hoá đơn đã huỷ";
            default -> "Chưa thu";
        };
    }

    private static String ticketStatusLabel(ServiceTicketJpa t) {
        if (t.getTicketStatus() == null) return "";
        return switch (t.getTicketStatus()) {
            case HOLDING -> "Đang giữ hàng";
            case CREATED -> "Mới tiếp nhận";
            case INSPECTING -> "Đang kiểm tra";
            case INSPECTED -> "Đã kiểm tra";
            case ESTIMATED -> "Đã báo giá";
            case PENDING -> "Chờ phụ tùng";
            case REPAIRING -> "Đang sửa chữa";
            case COMPLETED -> "Hoàn thành";
            case PAID -> "Đã thanh toán";
            case CANCELLED -> "Đã huỷ";
        };
    }

    private static String backfillLabel(ServiceTicketJpa t) {
        if (t.getEntryMode() != EntryMode.BACKFILL) return "";
        if (t.getBackfillReviewStatus() == null) return "Nhập bù";
        return switch (t.getBackfillReviewStatus().name()) {
            case "PENDING" -> "Nhập bù (chờ duyệt)";
            case "APPROVED" -> "Nhập bù (đã duyệt)";
            case "REJECTED" -> "Nhập bù (bị từ chối)";
            default -> "Nhập bù";
        };
    }

    private static String ticketTypeLabel(TicketType type) {
        return type == TicketType.PARTS_SALE ? "Bán phụ tùng" : "Sửa chữa";
    }

    private static String paymentMethodLabel(String method) {
        if (method == null || method.isBlank()) return "";
        return switch (method.trim().toUpperCase()) {
            case "CASH" -> "Tiền mặt";
            case "TRANSFER", "BANK_TRANSFER" -> "Chuyển khoản";
            case "CARD" -> "Thẻ";
            case "VNPAY" -> "VNPay";
            default -> method;
        };
    }

    /** SERVICE | PART | OTHER — cùng cách chia với RevenueReportService. */
    private static String kindOf(String itemType, String categoryType) {
        switch (nz(itemType).trim().toUpperCase()) {
            case "SERVICE", "COMBO", "MAINTENANCE_PACKAGE":
                return "SERVICE";
            case "PART", "EQUIPMENT", "MACHINERY":
                return "PART";
            default:
                break;
        }
        String ct = nz(categoryType).trim().toUpperCase();
        if (ct.startsWith("SERV")) return "SERVICE";
        if (ct.startsWith("PART")) return "PART";
        return "OTHER";
    }

    private static String kindLabel(String kind) {
        return switch (nz(kind)) {
            case "SERVICE" -> "Dịch vụ";
            case "PART" -> "Phụ tùng / vật tư";
            default -> "Khác";
        };
    }

    private static String itemTypeLabel(String type) {
        return switch (type) {
            case "PART" -> "Phụ tùng";
            case "EQUIPMENT" -> "Thiết bị";
            case "MACHINERY" -> "Máy móc";
            case "SERVICE" -> "Dịch vụ";
            case "COMBO" -> "Combo";
            case "MAINTENANCE_PACKAGE" -> "Gói bảo dưỡng";
            default -> type;
        };
    }

    private static String customerTypeLabel(String type) {
        return switch (nz(type).toUpperCase()) {
            case "DEALER" -> "Đại lý";
            case "GARAGE" -> "Garage";
            default -> "Cá nhân";
        };
    }

    private static String genderLabel(String gender) {
        return switch (nz(gender).toUpperCase()) {
            case "MALE" -> "Nam";
            case "FEMALE" -> "Nữ";
            case "OTHER" -> "Khác";
            default -> "";
        };
    }

    private static String rankLabel(String rank) {
        return switch (nz(rank).toUpperCase()) {
            case "BRONZE" -> "Đồng";
            case "SILVER" -> "Bạc";
            case "GOLD" -> "Vàng";
            case "PLATINUM" -> "Bạch kim";
            case "DIAMOND" -> "Kim cương";
            default -> nz(rank);
        };
    }

    // ==========================================================================================
    // Tiện ích

    /** Khách của phiếu — phiếu bán lẻ khách vãng lai ghi tên / SĐT ngay trên phiếu. */
    private static Person person(Scope s, TicketCtx c) {
        ServiceTicketJpa t = c.t;
        Person profile = t.getCustomerId() != null ? s.customers.get(t.getCustomerId()) : null;
        if (Boolean.TRUE.equals(t.getIsWalkIn())) {
            String name = nz(t.getWalkInName()).trim();
            return new Person(!name.isEmpty() ? name : WALK_IN, nz(t.getWalkInPhone()),
                    profile != null ? profile.code : "");
        }
        if (profile == null) return new Person(WALK_IN, "", "");
        return new Person(!profile.name.isBlank() ? profile.name : WALK_IN, profile.phone, profile.code);
    }

    private void queryIn(String sql, Collection<Integer> ids, RowCallbackHandler handler) {
        inChunks(ids, chunk -> jdbc.query(sql, new MapSqlParameterSource("ids", chunk), handler));
    }

    private void each(String sql, MapSqlParameterSource params, RowCallbackHandler handler) {
        jdbc.query(sql, params, handler);
    }

    /** IN (...) theo từng khúc — "từ trước đến nay" có thể hàng chục nghìn mã. */
    private static void inChunks(Collection<Integer> ids, Consumer<List<Integer>> action) {
        List<Integer> list = ids.stream().filter(Objects::nonNull).distinct().toList();
        for (int i = 0; i < list.size(); i += CHUNK) {
            action.accept(list.subList(i, Math.min(list.size(), i + CHUNK)));
        }
    }

    private static BigDecimal sumOf(Scope s, java.util.function.Function<TicketCtx, BigDecimal> f) {
        return s.tickets.stream().filter(c -> c.counted).map(f).reduce(ZERO, BigDecimal::add);
    }

    private static <T> List<T> sorted(List<T> list, Comparator<T> comparator) {
        List<T> copy = new ArrayList<>(list);
        copy.sort(comparator);
        return copy;
    }

    /** Tỉ lệ dạng phân số (Excel tự hiện %), null khi mẫu bằng 0. */
    private static BigDecimal ratio(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0 || part == null) return null;
        return part.divide(whole, 6, RoundingMode.HALF_UP);
    }

    private static String joinOr(Collection<String> names, String fallback) {
        if (names != null && !names.isEmpty()) return String.join(", ", names);
        return nz(fallback);
    }

    private static LocalDateTime latestOf(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    private static String nz(String value) {
        return value != null ? value : "";
    }

    private static String nz(Object value) {
        return value != null ? value.toString() : "";
    }

    // ==========================================================================================
    // Kiểu dữ liệu nội bộ

    private static final class Scope {
        boolean loaded;
        final List<TicketCtx> tickets = new ArrayList<>();
        /** ticket → item → hàng đã xuất kho. */
        final Map<Integer, Map<Integer, IssueAgg>> issues = new HashMap<>();
        final Map<Integer, Person> customers = new HashMap<>();
        final Map<Integer, Car> vehicles = new HashMap<>();
    }

    private static final class TicketCtx {
        final ServiceTicketJpa t;
        final ServiceBillJpa bill;
        Integer estimateId;
        List<Line> lines = List.of();
        boolean cancelled;
        /** Có hoá đơn hợp lệ và phiếu không huỷ → tính doanh thu. */
        boolean counted;
        boolean paid;
        /** Có dòng báo giá để chia tiền hoá đơn (tổng thành tiền dòng > 0). */
        boolean allocatable;
        boolean costIncomplete;
        LocalDateTime paidAt;
        BigDecimal revenue = ZERO;
        BigDecimal netRevenue = ZERO;
        BigDecimal vat = ZERO;
        BigDecimal goodsCost = ZERO;
        BigDecimal outsourceCost = ZERO;
        BigDecimal commission = ZERO;
        BigDecimal profit = ZERO;
        String advisor = "";
        String technician = "";
        String createdBy = "";
        String paymentMethod = "";
        Integer odometer;

        TicketCtx(ServiceTicketJpa t, ServiceBillJpa bill) {
            this.t = t;
            this.bill = bill;
        }
    }

    private static final class Line {
        Integer itemId;
        String name = "";
        String productName = "";
        String sku = "";
        String kind = "OTHER";
        String category = NO_CATEGORY;
        String unit = "";
        BigDecimal qty = ZERO;
        BigDecimal unitPrice = ZERO;
        BigDecimal totalPrice = ZERO;
        BigDecimal discount = ZERO;
        BigDecimal tax = ZERO;
        BigDecimal finalPrice = ZERO;
        BigDecimal laborCost = ZERO;
        boolean gift;
        boolean outsource;
        boolean tradeIn;
        boolean checked;
        BigDecimal allocated = ZERO;
        BigDecimal allocatedNet = ZERO;
    }

    private record IssueAgg(BigDecimal qty, BigDecimal cost, BigDecimal noLotQty, String name, String sku,
                            String itemType, String unit) {}

    private record Person(String name, String phone, String code) {}

    private record Car(String plate, String brand, String model) {}

    private record VehicleRow(int id, String plate, String brand, String model, Integer year) {}

    private record Visits(long count, LocalDateTime last, BigDecimal amount) {}

    private static final class ProductAgg {
        final String sku;
        final String name;
        final String kind;
        final String category;
        final String unit;
        final Set<Integer> tickets = new HashSet<>();
        BigDecimal qty = ZERO;
        BigDecimal giftQty = ZERO;
        BigDecimal gross = ZERO;
        BigDecimal discount = ZERO;
        BigDecimal tax = ZERO;
        BigDecimal lineTotal = ZERO;
        BigDecimal revenue = ZERO;
        BigDecimal paidRevenue = ZERO;
        BigDecimal netRevenue = ZERO;
        BigDecimal cost = ZERO;
        BigDecimal issuedQty = ZERO;
        BigDecimal outsource = ZERO;
        boolean costIncomplete;

        ProductAgg(String sku, String name, String kind, String category, String unit) {
            this.sku = nz(sku);
            this.name = nz(name);
            this.kind = kind;
            this.category = category;
            this.unit = nz(unit);
        }

        BigDecimal profit() {
            return netRevenue.subtract(cost).subtract(outsource);
        }
    }

    private static final class ItemRow {
        int id;
        String sku;
        String partNumber;
        String name;
        String type;
        String unit;
        String category;
        BigDecimal price;
        boolean hasInventory;
        BigDecimal qty = ZERO;
        BigDecimal reserved = ZERO;
        BigDecimal defectQty = ZERO;
        int minLevel;
        BigDecimal lotQty = ZERO;
        BigDecimal lotValue = ZERO;
        BigDecimal inQty = ZERO;
        BigDecimal outQty = ZERO;
        final List<String> byWarehouse = new ArrayList<>();
    }

    private static final class CustomerRow {
        int id;
        String code;
        String name;
        String phone;
        String email;
        LocalDate dob;
        String gender;
        String type;
        String companyName;
        String taxCode;
        String address;
        String identityCard;
        String groupName;
        String channel;
        boolean doNotContact;
        String contactName;
        String contactPhone;
        String note;
        LocalDateTime createdAt;
        String rank;
        Integer points;

        static CustomerRow of(ResultSet rs) throws SQLException {
            CustomerRow c = new CustomerRow();
            c.id = rs.getInt("customer_id");
            c.code = nz(rs.getString("customer_code"));
            c.name = nz(rs.getString("full_name"));
            c.phone = nz(rs.getString("phone")).trim();
            c.email = nz(rs.getString("email"));
            c.dob = rs.getObject("dob", LocalDate.class);
            c.gender = nz(rs.getString("gender"));
            c.type = nz(rs.getString("customer_type"));
            String company = nz(rs.getString("company_name")).trim();
            c.companyName = rs.getBoolean("is_company") || !company.isEmpty() ? company : "";
            c.taxCode = nz(rs.getString("tax_code"));
            c.address = Stream.of(rs.getString("address"), rs.getString("ward_name"),
                            rs.getString("district_name"), rs.getString("province_name"))
                    .filter(v -> v != null && !v.isBlank()).map(String::trim)
                    .collect(Collectors.joining(", "));
            c.identityCard = nz(rs.getString("identity_card"));
            c.groupName = nz(rs.getString("group_name"));
            String channel = nz(rs.getString("notification_channel")).toUpperCase();
            c.channel = "EMAIL".equals(channel) ? "Email" : "ZALO".equals(channel) ? "Zalo" : channel;
            c.doNotContact = rs.getBoolean("do_not_contact");
            c.contactName = nz(rs.getString("contact_name"));
            c.contactPhone = nz(rs.getString("contact_phone"));
            c.note = nz(rs.getString("note"));
            c.createdAt = rs.getObject("created_at", LocalDateTime.class);
            c.rank = nz(rs.getString("current_rank"));
            c.points = rs.getObject("total_points", Integer.class);
            return c;
        }
    }

    /** Số liệu tổng hợp các tab không theo phiếu, để in lên Tổng quan. */
    private static final class Overview {
        int customers;
        long activeCustomers;
        int vehicles;
        int items;
        long outOfStock;
        long belowMin;
        BigDecimal stockValue = ZERO;
    }
}
