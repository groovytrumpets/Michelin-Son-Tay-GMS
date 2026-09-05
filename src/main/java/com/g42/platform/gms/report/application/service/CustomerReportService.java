package com.g42.platform.gms.report.application.service;

import com.g42.platform.gms.auth.api.internal.CustomerInternalApi;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.billing.domain.enums.PaymentStatus;
import com.g42.platform.gms.billing.infrastructure.entity.ServiceBillJpa;
import com.g42.platform.gms.billing.infrastructure.repository.ServiceBillJpaRepo;
import com.g42.platform.gms.dashboard.api.dto.DashboardRevenueResponseDto;
import com.g42.platform.gms.dashboard.application.service.DashboardRevenueService;
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

/**
 * Tổng hợp báo cáo khách hàng theo ngày: 1 ngày (hoặc 1 khoảng) có bao nhiêu khách làm dịch vụ
 * và thu được bao nhiêu tiền. Nguồn dữ liệu:
 *  - {@code service_ticket} (lọc theo received_at) → danh sách khách + số phiếu.
 *  - {@code service_bill}   → số tiền thu theo từng phiếu.
 *  - {@link DashboardRevenueService} → đối chiếu tổng thu với bảng doanh thu tổng hợp.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerReportService {

    private static final BigDecimal ONE_POINT_ONE = new BigDecimal("1.1");

    private final ServiceTicketRepo serviceTicketRepo;
    private final ServiceBillJpaRepo serviceBillJpaRepo;
    private final CustomerInternalApi customerInternalApi;
    private final VehicleInternalApi vehicleInternalApi;
    private final DashboardRevenueService dashboardRevenueService;

    @Transactional(readOnly = true)
    public CustomerReportResponse buildReport(LocalDate fromDate, LocalDate toDate) {
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
                .toList();

        List<Integer> customerIds = tickets.stream()
                .map(ServiceTicket::getCustomerId).filter(Objects::nonNull).distinct().toList();
        List<Integer> vehicleIds = tickets.stream()
                .map(ServiceTicket::getVehicleId).filter(Objects::nonNull).distinct().toList();
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

        List<TicketRow> ticketRows = new ArrayList<>(tickets.size());
        for (ServiceTicket ticket : tickets) {
            ServiceBillJpa bill = billByTicketId.get(ticket.getServiceTicketId());
            CustomerProfile customer = ticket.getCustomerId() != null ? customerMap.get(ticket.getCustomerId()) : null;
            Vehicle vehicle = ticket.getVehicleId() != null ? vehicleMap.get(ticket.getVehicleId()) : null;

            boolean paid = (bill != null && bill.getPaymentStatus() == PaymentStatus.PAID)
                    || ticket.getTicketStatus() == TicketStatus.PAID;

            ticketRows.add(TicketRow.builder()
                    .serviceTicketId(ticket.getServiceTicketId())
                    .ticketCode(nz(ticket.getTicketCode()))
                    .date(ticket.getReceivedAt() != null ? ticket.getReceivedAt().toLocalDate() : from)
                    .customerName(customer != null ? nz(customer.getFullName()) : "")
                    .customerPhone(customer != null ? nz(customer.getPhone()) : "")
                    .licensePlate(vehicle != null ? nz(vehicle.getLicensePlate()) : "")
                    .ticketStatus(ticket.getTicketStatus() != null ? ticket.getTicketStatus().name() : "")
                    .ticketType(ticket.getTicketType() != null ? ticket.getTicketType().name() : "")
                    .paid(paid)
                    .hasBill(bill != null)
                    .revenue(bill != null ? nvl(bill.getFinalAmount()) : BigDecimal.ZERO)
                    .discountAmount(bill != null ? nvl(bill.getDiscountAmount()) : BigDecimal.ZERO)
                    .build());
        }

        List<CustomerRow> customers = aggregateByCustomer(tickets, ticketRows, customerMap);

        BigDecimal revenueFromBills = ticketRows.stream()
                .map(TicketRow::getRevenue).map(CustomerReportService::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountTotal = ticketRows.stream()
                .map(TicketRow::getDiscountAmount).map(CustomerReportService::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal kpiPaidRevenue = fetchDashboardPaidRevenue(from, to);
        boolean useDashboard = kpiPaidRevenue.signum() > 0;
        BigDecimal totalRevenue = useDashboard ? kpiPaidRevenue : revenueFromBills;

        long paidTickets = ticketRows.stream().filter(TicketRow::isPaid).count();
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
                .revenueSource(useDashboard ? "dashboard" : "bills")
                .build();

        return CustomerReportResponse.builder()
                .summary(summary)
                .customers(customers)
                .tickets(ticketRows)
                .build();
    }

    /** Xuất báo cáo ra file Excel (.xlsx) gồm 3 sheet: Tổng quan / Theo khách hàng / Chi tiết phiếu. */
    public byte[] exportReport(LocalDate fromDate, LocalDate toDate) {
        CustomerReportResponse report = buildReport(fromDate, toDate);
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

    private List<CustomerRow> aggregateByCustomer(List<ServiceTicket> tickets,
                                                 List<TicketRow> ticketRows,
                                                 Map<Integer, CustomerProfile> customerMap) {
        // ticketRows song song 1-1 với tickets
        Map<String, CustomerAccumulator> acc = new LinkedHashMap<>();
        for (int i = 0; i < tickets.size(); i++) {
            ServiceTicket ticket = tickets.get(i);
            TicketRow row = ticketRows.get(i);
            CustomerProfile customer = ticket.getCustomerId() != null ? customerMap.get(ticket.getCustomerId()) : null;

            String phone = customer != null ? nz(customer.getPhone()).replaceAll("\\s+", "") : "";
            String name = customer != null ? nz(customer.getFullName()) : "";
            String key = !phone.isEmpty() ? "p:" + phone
                    : !name.isEmpty() ? "n:" + name.toLowerCase()
                    : "c:" + ticket.getCustomerId();

            CustomerAccumulator a = acc.computeIfAbsent(key, k -> {
                CustomerAccumulator na = new CustomerAccumulator();
                na.customerId = ticket.getCustomerId();
                na.customerName = name.isEmpty() ? "Khách lẻ" : name;
                na.customerPhone = phone;
                return na;
            });
            a.ticketCount++;
            if (row.isPaid()) {
                a.paidTicketCount++;
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
                        .build())
                .sorted(Comparator.comparing(CustomerRow::getRevenue, Comparator.reverseOrder())
                        .thenComparing(CustomerRow::getTicketCount, Comparator.reverseOrder()))
                .toList();
    }

    private BigDecimal fetchDashboardPaidRevenue(LocalDate from, LocalDate to) {
        try {
            DashboardRevenueResponseDto dto = dashboardRevenueService.getRevenueReport(from, to);
            if (dto != null && dto.getKpis() != null && dto.getKpis().getPaidRevenue() != null) {
                return dto.getKpis().getPaidRevenue();
            }
        } catch (Exception e) {
            log.warn("Không lấy được doanh thu tổng hợp cho báo cáo khách hàng: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
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
        putRow(sheet, r, "Trung bình/khách (VND)", s.getAveragePerCustomer());
        sheet.setColumnWidth(0, 9000);
        sheet.setColumnWidth(1, 8000);
    }

    private void writeCustomerSheet(Workbook workbook, List<CustomerRow> customers) {
        Sheet sheet = workbook.createSheet("Theo khách hàng");
        String[] headers = {"STT", "Khách hàng", "Số điện thoại", "Biển số", "Số phiếu",
                "Phiếu đã thu", "Doanh thu (VND)", "Giảm giá (VND)"};
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
                    nvl(c.getDiscountAmount()));
        }
        autoSize(sheet, headers.length);
    }

    private void writeTicketSheet(Workbook workbook, List<TicketRow> ticketRows) {
        Sheet sheet = workbook.createSheet("Chi tiết phiếu");
        String[] headers = {"STT", "Mã phiếu", "Ngày", "Khách hàng", "Số điện thoại", "Biển số",
                "Trạng thái", "Đã thanh toán", "Doanh thu (VND)", "Giảm giá (VND)"};
        putRow(sheet, 0, (Object[]) headers);
        int rowIdx = 1;
        int stt = 1;
        for (TicketRow t : ticketRows) {
            putRow(sheet, rowIdx++,
                    stt++,
                    t.getTicketCode(),
                    t.getDate() != null ? t.getDate().toString() : "",
                    t.getCustomerName(),
                    t.getCustomerPhone(),
                    t.getLicensePlate(),
                    t.getTicketStatus(),
                    t.isPaid() ? "Có" : "Không",
                    nvl(t.getRevenue()),
                    nvl(t.getDiscountAmount()));
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
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal discountAmount = BigDecimal.ZERO;
        private final java.util.LinkedHashSet<String> plates = new java.util.LinkedHashSet<>();
        private final List<String> ticketCodes = new ArrayList<>();
    }
}
