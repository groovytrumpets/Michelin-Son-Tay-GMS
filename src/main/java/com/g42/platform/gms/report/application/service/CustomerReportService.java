package com.g42.platform.gms.report.application.service;

import com.g42.platform.gms.auth.api.internal.CustomerInternalApi;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.billing.domain.enums.PaymentStatus;
import com.g42.platform.gms.billing.infrastructure.entity.ServiceBillJpa;
import com.g42.platform.gms.billing.infrastructure.repository.ServiceBillJpaRepo;
import com.g42.platform.gms.customerimport.infrastructure.entity.LegacyVisitJpa;
import com.g42.platform.gms.customerimport.infrastructure.repository.LegacyVisitRepository;
import com.g42.platform.gms.report.api.dto.CustomerReportResponse;
import com.g42.platform.gms.report.api.dto.CustomerReportResponse.CustomerRow;
import com.g42.platform.gms.report.api.dto.CustomerReportResponse.Summary;
import com.g42.platform.gms.report.api.dto.CustomerReportResponse.TicketRow;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import com.g42.platform.gms.vehicle.api.internal.VehicleInternalApi;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tổng hợp báo cáo khách hàng theo ngày: 1 ngày (hoặc 1 khoảng) có bao nhiêu khách làm dịch vụ
 * và thu được bao nhiêu tiền. Nguồn dữ liệu:
 *  - {@code service_ticket} (lọc theo received_at) → danh sách khách + số phiếu.
 *  - {@code service_bill}   → số tiền thu theo từng phiếu.
 *  - {@code legacy_visit}   → lượt khách nhập từ sổ Excel cũ, khi {@code includeLegacy}.
 *
 * Về tiền của sổ cũ: {@code legacy_visit.total_amount} là con số chép tay, không có hoá đơn
 * đằng sau, nên nó KHÔNG được trộn vào doanh thu kế toán ở những báo cáo khác. Ở đây nó có
 * mặt vì báo cáo này trả lời câu hỏi "cả xưởng đã làm cho bao nhiêu khách, thu bao nhiêu",
 * nhưng luôn đi kèm cột "Nguồn" và số tách riêng ({@code legacyRevenue} / {@code systemRevenue})
 * để người xem biết phần nào là hoá đơn thật.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerReportService {

    private static final BigDecimal ONE_POINT_ONE = new BigDecimal("1.1");
    private static final String SOURCE_SYSTEM = "SYSTEM";
    private static final String SOURCE_LEGACY = "LEGACY";

    private final ServiceTicketRepo serviceTicketRepo;
    private final ServiceBillJpaRepo serviceBillJpaRepo;
    private final LegacyVisitRepository legacyVisitRepository;
    private final CustomerInternalApi customerInternalApi;
    private final VehicleInternalApi vehicleInternalApi;
    private final com.g42.platform.gms.branch.service.BranchDirectory branchDirectory;

    @Transactional(readOnly = true)
    public CustomerReportResponse buildReport(LocalDate fromDate, LocalDate toDate, boolean includeLegacy) {
        return buildReport(fromDate, toDate, includeLegacy, null);
    }

    /**
     * @param branchId null = mọi xưởng. Sổ cũ chỉ tính khi lọc đúng xưởng mặc định (xem
     *                 RevenueReportService). Khi lọc theo xưởng thì doanh thu cộng từ hoá đơn,
     *                 không lấy con số của dashboard vì con số đó là của cả hệ thống.
     */
    @Transactional(readOnly = true)
    public CustomerReportResponse buildReport(LocalDate fromDate, LocalDate toDate, boolean includeLegacy,
                                              Integer branchId) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now();
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        if (from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        List<ServiceTicket> tickets = serviceTicketRepo.findBetween(start, end).stream()
                .filter(t -> t != null && !Boolean.TRUE.equals(t.getIsDeleted()))
                .filter(t -> branchId == null || branchId.equals(t.getBranchId()))
                .toList();

        Integer defaultBranchId = branchDirectory.defaultBranchId();
        boolean legacyInBranch = branchId == null || branchId.equals(defaultBranchId);
        List<LegacyVisitJpa> legacyVisits = includeLegacy && legacyInBranch
                ? legacyVisitRepository.findByVisitedAtBetweenOrderByVisitedAtAsc(start, end)
                : List.<LegacyVisitJpa>of();

        List<Integer> customerIds = Stream.concat(
                        tickets.stream().map(ServiceTicket::getCustomerId),
                        legacyVisits.stream().map(LegacyVisitJpa::getCustomerId))
                .filter(Objects::nonNull).distinct().toList();
        List<Integer> vehicleIds = Stream.concat(
                        tickets.stream().map(ServiceTicket::getVehicleId),
                        legacyVisits.stream().map(LegacyVisitJpa::getVehicleId))
                .filter(Objects::nonNull).distinct().toList();
        List<Integer> ticketIds = tickets.stream()
                .map(ServiceTicket::getServiceTicketId).filter(Objects::nonNull).toList();

        Map<Integer, CustomerProfile> customerMap = customerIds.isEmpty()
                ? Map.of()
                : customerInternalApi.findAllByIds(customerIds).stream()
                    .filter(c -> c != null && c.getCustomerId() != null)
                    .collect(Collectors.toMap(CustomerProfile::getCustomerId, c -> c, (a, b) -> a));
        Map<Integer, Vehicle> vehicleMap = vehicleIds.isEmpty()
                ? Map.of()
                : vehicleInternalApi.findAllByIds(vehicleIds).stream()
                    .filter(v -> v != null && v.getVehicleId() != null)
                    .collect(Collectors.toMap(Vehicle::getVehicleId, v -> v, (a, b) -> a));

        Map<Integer, ServiceBillJpa> billByTicketId = latestBillPerTicket(ticketIds);

        List<KeyedRow> keyedRows = new ArrayList<>(tickets.size() + legacyVisits.size());

        for (ServiceTicket ticket : tickets) {
            ServiceBillJpa bill = billByTicketId.get(ticket.getServiceTicketId());
            CustomerProfile customer = ticket.getCustomerId() != null ? customerMap.get(ticket.getCustomerId()) : null;
            Vehicle vehicle = ticket.getVehicleId() != null ? vehicleMap.get(ticket.getVehicleId()) : null;

            boolean paid = (bill != null && bill.getPaymentStatus() == PaymentStatus.PAID)
                    || ticket.getTicketStatus() == TicketStatus.PAID;

            // Phiếu bán lẻ khách vãng lai: hồ sơ gắn vào là hồ sơ dùng chung "Khách lẻ",
            // tên/SĐT thật nằm trên phiếu.
            boolean walkIn = Boolean.TRUE.equals(ticket.getIsWalkIn());

            TicketRow row = TicketRow.builder()
                    .serviceTicketId(ticket.getServiceTicketId())
                    .ticketCode(nz(ticket.getTicketCode()))
                    .date(ticket.getReceivedAt() != null ? ticket.getReceivedAt().toLocalDate() : from)
                    .customerName(walkIn ? nz(ticket.getWalkInName()) : (customer != null ? nz(customer.getFullName()) : ""))
                    .customerPhone(walkIn ? nz(ticket.getWalkInPhone()) : (customer != null ? nz(customer.getPhone()) : ""))
                    .licensePlate(vehicle != null ? nz(vehicle.getLicensePlate()) : "")
                    .ticketStatus(ticket.getTicketStatus() != null ? ticket.getTicketStatus().name() : "")
                    .ticketType(ticket.getTicketType() != null ? ticket.getTicketType().name() : "")
                    .branchName(branchDirectory.nameOf(ticket.getBranchId()))
                    .paid(paid)
                    .hasBill(bill != null)
                    .revenue(bill != null ? nvl(bill.getFinalAmount()) : BigDecimal.ZERO)
                    .discountAmount(bill != null ? nvl(bill.getDiscountAmount()) : BigDecimal.ZERO)
                    .source(SOURCE_SYSTEM)
                    .amountMismatch(false)
                    .build();
            // Mỗi phiếu khách lẻ là một lượt khách khác nhau, dù chung một hồ sơ — gom
            // theo hồ sơ sẽ biến cả tháng bán lẻ thành đúng một khách.
            String key = walkIn ? "w:" + ticket.getServiceTicketId() : customerKey(customer, ticket.getCustomerId());
            keyedRows.add(new KeyedRow(row, key, ticket.getCustomerId(), false));
        }

        for (LegacyVisitJpa visit : legacyVisits) {
            CustomerProfile customer = visit.getCustomerId() != null ? customerMap.get(visit.getCustomerId()) : null;
            Vehicle vehicle = visit.getVehicleId() != null ? vehicleMap.get(visit.getVehicleId()) : null;
            BigDecimal amount = nvl(visit.getTotalAmount());

            TicketRow row = TicketRow.builder()
                    .serviceTicketId(null)
                    .ticketCode(legacyCode(visit))
                    .date(visit.getVisitedAt() != null ? visit.getVisitedAt().toLocalDate() : from)
                    .customerName(customer != null ? nz(customer.getFullName()) : "")
                    .customerPhone(customer != null ? nz(customer.getPhone()) : "")
                    .licensePlate(vehicle != null ? nz(vehicle.getLicensePlate()) : "")
                    .ticketStatus("LEGACY")
                    .ticketType("")
                    .branchName(branchDirectory.nameOf(defaultBranchId))
                    // Sổ cũ chỉ ghi lượt đã làm xong: có tiền trong sổ nghĩa là đã thu.
                    .paid(amount.signum() > 0)
                    .hasBill(false)
                    .revenue(amount)
                    .discountAmount(nvl(visit.getDiscountAmount()))
                    .source(SOURCE_LEGACY)
                    .legacyVisitId(visit.getLegacyVisitId())
                    .servicesText(nz(visit.getServicesText()))
                    .amountMismatch(Boolean.TRUE.equals(visit.getAmountMismatch()))
                    .build();
            keyedRows.add(new KeyedRow(row, customerKey(customer, visit.getCustomerId()), visit.getCustomerId(), true));
        }

        keyedRows.sort(Comparator.comparing((KeyedRow r) -> r.row.getDate(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(r -> r.row.getSource()));

        List<TicketRow> ticketRows = keyedRows.stream().map(r -> r.row).toList();
        List<CustomerRow> customers = aggregateByCustomer(keyedRows);

        BigDecimal revenueFromBills = sumRevenue(keyedRows, false);
        BigDecimal legacyRevenue = sumRevenue(keyedRows, true);
        BigDecimal discountTotal = ticketRows.stream()
                .map(TicketRow::getDiscountAmount).map(CustomerReportService::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal systemRevenue = revenueFromBills;
        BigDecimal totalRevenue = systemRevenue.add(legacyRevenue);

        long paidTickets = ticketRows.stream().filter(TicketRow::isPaid).count();
        long legacyOnlyCustomers = customers.stream()
                .filter(c -> c.getLegacyVisitCount() > 0 && c.getLegacyVisitCount() == c.getTicketCount())
                .count();
        BigDecimal averagePerCustomer = customers.isEmpty()
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(customers.size()), 0, RoundingMode.HALF_UP);

        Summary summary = Summary.builder()
                .from(from)
                .to(to)
                .totalCustomers(customers.size())
                .totalTickets(ticketRows.size())
                .paidTickets(paidTickets)
                .unpaidTickets(ticketRows.size() - paidTickets)
                .totalRevenue(totalRevenue)
                .revenueFromBills(revenueFromBills)
                .discountTotal(discountTotal)
                .averagePerCustomer(averagePerCustomer)
                .revenueSource("bills")
                .legacyIncluded(includeLegacy)
                .legacyVisits(legacyVisits.size())
                .legacyOnlyCustomers(legacyOnlyCustomers)
                .legacyRevenue(legacyRevenue)
                .systemRevenue(systemRevenue)
                .build();

        return CustomerReportResponse.builder()
                .summary(summary)
                .customers(customers)
                .tickets(ticketRows)
                .build();
    }

    /** Xuất báo cáo ra file Excel (.xlsx) gồm 3 sheet: Tổng quan / Theo khách hàng / Chi tiết phiếu. */
    public byte[] exportReport(LocalDate fromDate, LocalDate toDate, boolean includeLegacy) {
        return exportReport(fromDate, toDate, includeLegacy, null);
    }

    public byte[] exportReport(LocalDate fromDate, LocalDate toDate, boolean includeLegacy, Integer branchId) {
        CustomerReportResponse report = buildReport(fromDate, toDate, includeLegacy, branchId);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeOverviewSheet(workbook, report.getSummary());
            writeCustomerSheet(workbook, report.getCustomers());
            writeTicketSheet(workbook, report.getTickets());
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Xuất Excel báo cáo khách hàng thất bại", e);
            throw new RuntimeException("Không thể tạo file Excel báo cáo khách hàng: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------------------------------

    private Map<Integer, ServiceBillJpa> latestBillPerTicket(List<Integer> ticketIds) {
        Map<Integer, ServiceBillJpa> result = new HashMap<>();
        if (ticketIds.isEmpty()) {
            return result;
        }
        for (ServiceBillJpa bill : serviceBillJpaRepo.findByServiceTicketIdIn(ticketIds)) {
            if (bill == null || bill.getServiceTicketId() == null) {
                continue;
            }
            result.merge(bill.getServiceTicketId(), bill,
                    (existing, candidate) -> isNewerBill(candidate, existing) ? candidate : existing);
        }
        return result;
    }

    private static boolean isNewerBill(ServiceBillJpa candidate, ServiceBillJpa current) {
        Instant a = candidate.getPaidAt();
        Instant b = current.getPaidAt();
        if (a != null && b != null && !a.equals(b)) {
            return a.isAfter(b);
        }
        if (a != null && b == null) {
            return true;
        }
        if (a == null && b != null) {
            return false;
        }
        int idA = candidate.getBillId() != null ? candidate.getBillId() : Integer.MIN_VALUE;
        int idB = current.getBillId() != null ? current.getBillId() : Integer.MIN_VALUE;
        return idA > idB;
    }

    /**
     * Gộp phiếu trong phần mềm và lượt sổ cũ về cùng một khách. Khoá gộp ưu tiên số điện
     * thoại nên một khách vừa có lịch sử sổ cũ vừa có phiếu mới chỉ hiện một dòng.
     */
    private List<CustomerRow> aggregateByCustomer(List<KeyedRow> keyedRows) {
        Map<String, CustomerAccumulator> acc = new LinkedHashMap<>();
        for (KeyedRow keyed : keyedRows) {
            TicketRow row = keyed.row;

            CustomerAccumulator a = acc.computeIfAbsent(keyed.customerKey, k -> {
                CustomerAccumulator na = new CustomerAccumulator();
                na.customerId = keyed.customerId;
                na.customerName = row.getCustomerName().isBlank() ? "Khách lẻ" : row.getCustomerName();
                na.customerPhone = nz(row.getCustomerPhone()).replaceAll("\\s+", "");
                return na;
            });
            a.ticketCount++;
            if (row.isPaid()) {
                a.paidTicketCount++;
            }
            if (keyed.legacy) {
                a.legacyVisitCount++;
                a.legacyRevenue = a.legacyRevenue.add(nvl(row.getRevenue()));
            }
            a.revenue = a.revenue.add(nvl(row.getRevenue()));
            a.discountAmount = a.discountAmount.add(nvl(row.getDiscountAmount()));
            if (row.getLicensePlate() != null && !row.getLicensePlate().isBlank()) {
                a.plates.add(row.getLicensePlate());
            }
            if (row.getTicketCode() != null && !row.getTicketCode().isBlank()) {
                a.ticketCodes.add(row.getTicketCode());
            }
        }

        return acc.values().stream()
                .map(a -> CustomerRow.builder()
                        .customerId(a.customerId)
                        .customerName(a.customerName)
                        .customerPhone(a.customerPhone)
                        .plates(new ArrayList<>(a.plates))
                        .ticketCount(a.ticketCount)
                        .paidTicketCount(a.paidTicketCount)
                        .revenue(a.revenue)
                        .discountAmount(a.discountAmount)
                        .ticketCodes(a.ticketCodes)
                        .legacyVisitCount(a.legacyVisitCount)
                        .legacyRevenue(a.legacyRevenue)
                        .build())
                .sorted(Comparator.comparing(CustomerRow::getRevenue, Comparator.reverseOrder())
                        .thenComparing(CustomerRow::getTicketCount, Comparator.reverseOrder()))
                .toList();
    }

    /** Khoá gộp khách: ưu tiên SĐT, rồi đến tên, cuối cùng mới là mã khách. */
    private static String customerKey(CustomerProfile customer, Integer customerId) {
        String phone = customer != null ? nz(customer.getPhone()).replaceAll("\\s+", "") : "";
        if (!phone.isEmpty()) {
            return "p:" + phone;
        }
        String name = customer != null ? nz(customer.getFullName()).trim() : "";
        if (!name.isEmpty()) {
            return "n:" + name.toLowerCase();
        }
        return "c:" + customerId;
    }

    /** Lượt sổ cũ thường không có mã phiếu; khi đó dựng mã tạm để dòng nào cũng có định danh. */
    private static String legacyCode(LegacyVisitJpa visit) {
        String code = nz(visit.getLegacyTicketCode()).trim();
        return !code.isEmpty() ? code : "SC-" + visit.getLegacyVisitId();
    }

    private static BigDecimal sumRevenue(List<KeyedRow> rows, boolean legacy) {
        return rows.stream()
                .filter(r -> r.legacy == legacy)
                .map(r -> nvl(r.row.getRevenue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------------------------------------
    // Excel

    private void writeOverviewSheet(Workbook workbook, Summary s) {
        Sheet sheet = workbook.createSheet("Tổng quan");
        int r = 0;
        putRow(sheet, r++, "BÁO CÁO KHÁCH HÀNG");
        putRow(sheet, r++, "Kỳ báo cáo", rangeLabel(s.getFrom(), s.getTo()));
        putRow(sheet, r++, "Xuất lúc", LocalDateTime.now().toString());
        r++;
        putRow(sheet, r++, "Chỉ số", "Giá trị");
        putRow(sheet, r++, "Tổng khách hàng", s.getTotalCustomers());
        putRow(sheet, r++, "Tổng lượt phiếu dịch vụ", s.getTotalTickets());
        putRow(sheet, r++, "Phiếu đã thanh toán", s.getPaidTickets());
        putRow(sheet, r++, "Phiếu chưa thanh toán", s.getUnpaidTickets());
        putRow(sheet, r++, "Tổng thu (VND)", s.getTotalRevenue());
        putRow(sheet, r++, "Tổng thu chưa VAT (VND)", noTax(s.getTotalRevenue()));
        putRow(sheet, r++, "Tổng giảm giá (VND)", s.getDiscountTotal());
        putRow(sheet, r++, "Trung bình/khách (VND)", s.getAveragePerCustomer());
        r++;
        putRow(sheet, r++, "Trong đó — phiếu trong phần mềm");
        putRow(sheet, r++, "Lượt phiếu phần mềm", s.getTotalTickets() - s.getLegacyVisits());
        putRow(sheet, r++, "Tiền theo hoá đơn (VND)", s.getSystemRevenue());
        putRow(sheet, r++, "Trong đó — sổ dịch vụ cũ");
        putRow(sheet, r++, "Có gộp sổ cũ", s.isLegacyIncluded() ? "Có" : "Không");
        putRow(sheet, r++, "Lượt từ sổ cũ", s.getLegacyVisits());
        putRow(sheet, r++, "Khách chỉ có sổ cũ", s.getLegacyOnlyCustomers());
        putRow(sheet, r, "Tiền ghi trong sổ cũ (VND)", s.getLegacyRevenue());
        sheet.setColumnWidth(0, 9000);
        sheet.setColumnWidth(1, 8000);
    }

    private void writeCustomerSheet(Workbook workbook, List<CustomerRow> customers) {
        Sheet sheet = workbook.createSheet("Theo khách hàng");
        String[] headers = {"STT", "Khách hàng", "Số điện thoại", "Biển số", "Số phiếu",
                "Phiếu đã thu", "Doanh thu (VND)", "Giảm giá (VND)",
                "Lượt sổ cũ", "Tiền sổ cũ (VND)"};
        putRow(sheet, 0, (Object[]) headers);
        int rowIdx = 1;
        int stt = 1;
        for (CustomerRow c : customers) {
            putRow(sheet, rowIdx++,
                    stt++,
                    c.getCustomerName(),
                    c.getCustomerPhone(),
                    String.join(", ", c.getPlates()),
                    c.getTicketCount(),
                    c.getPaidTicketCount(),
                    nvl(c.getRevenue()),
                    nvl(c.getDiscountAmount()),
                    c.getLegacyVisitCount(),
                    nvl(c.getLegacyRevenue()));
        }
        autoSize(sheet, headers.length);
    }

    private void writeTicketSheet(Workbook workbook, List<TicketRow> ticketRows) {
        Sheet sheet = workbook.createSheet("Chi tiết phiếu");
        String[] headers = {"STT", "Nguồn", "Mã phiếu", "Ngày", "Khách hàng", "Số điện thoại", "Biển số",
                "Trạng thái", "Đã thanh toán", "Doanh thu (VND)", "Giảm giá (VND)", "Nội dung sổ cũ"};
        putRow(sheet, 0, (Object[]) headers);
        int rowIdx = 1;
        int stt = 1;
        for (TicketRow t : ticketRows) {
            boolean legacy = SOURCE_LEGACY.equals(t.getSource());
            putRow(sheet, rowIdx++,
                    stt++,
                    legacy ? "Sổ cũ" : "Phần mềm",
                    t.getTicketCode(),
                    t.getDate() != null ? t.getDate().toString() : "",
                    t.getCustomerName(),
                    t.getCustomerPhone(),
                    t.getLicensePlate(),
                    t.getTicketStatus(),
                    t.isPaid() ? "Có" : "Không",
                    nvl(t.getRevenue()),
                    nvl(t.getDiscountAmount()),
                    legacy ? nz(t.getServicesText()) : "");
        }
        autoSize(sheet, headers.length);
    }

    private static void putRow(Sheet sheet, int rowIdx, Object... values) {
        Row row = sheet.createRow(rowIdx);
        for (int c = 0; c < values.length; c++) {
            Cell cell = row.createCell(c);
            Object value = values[c];
            if (value instanceof Number n) {
                cell.setCellValue(n.doubleValue());
            } else if (value instanceof Boolean b) {
                cell.setCellValue(b);
            } else if (value != null) {
                cell.setCellValue(value.toString());
            }
        }
    }

    private static void autoSize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(width + 512, 20000));
        }
    }

    private static String rangeLabel(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return "";
        }
        return from.equals(to) ? from.toString() : from + " - " + to;
    }

    private static BigDecimal noTax(BigDecimal withTax) {
        return nvl(withTax).divide(ONE_POINT_ONE, 0, RoundingMode.HALF_UP);
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static String nz(String value) {
        return value != null ? value : "";
    }

    private static final class CustomerAccumulator {
        private Integer customerId;
        private String customerName;
        private String customerPhone;
        private long ticketCount;
        private long paidTicketCount;
        private long legacyVisitCount;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal legacyRevenue = BigDecimal.ZERO;
        private BigDecimal discountAmount = BigDecimal.ZERO;
        private final java.util.LinkedHashSet<String> plates = new java.util.LinkedHashSet<>();
        private final List<String> ticketCodes = new ArrayList<>();
    }

    /** Một dòng báo cáo kèm khoá gộp khách, để hai nguồn dữ liệu đi chung một vòng lặp. */
    private static final class KeyedRow {
        private final TicketRow row;
        private final String customerKey;
        private final Integer customerId;
        private final boolean legacy;

        private KeyedRow(TicketRow row, String customerKey, Integer customerId, boolean legacy) {
            this.row = row;
            this.customerKey = customerKey;
            this.customerId = customerId;
            this.legacy = legacy;
        }
    }
}
