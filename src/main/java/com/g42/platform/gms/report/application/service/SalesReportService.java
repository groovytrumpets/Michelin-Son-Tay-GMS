package com.g42.platform.gms.report.application.service;

import com.g42.platform.gms.auth.api.internal.CustomerInternalApi;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.billing.infrastructure.entity.ServiceBillJpa;
import com.g42.platform.gms.customerimport.infrastructure.entity.LegacyVisitItemJpa;
import com.g42.platform.gms.customerimport.infrastructure.entity.LegacyVisitJpa;
import com.g42.platform.gms.customerimport.infrastructure.repository.LegacyVisitItemRepository;
import com.g42.platform.gms.report.api.dto.SalesReportResponse;
import com.g42.platform.gms.report.api.dto.SalesReportResponse.CustomerRow;
import com.g42.platform.gms.report.api.dto.SalesReportResponse.DetailRow;
import com.g42.platform.gms.report.api.dto.SalesReportResponse.ProductRow;
import com.g42.platform.gms.report.api.dto.SalesReportResponse.Summary;
import com.g42.platform.gms.report.application.service.RevenueReportService.PeriodBill;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketType;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.vehicle.api.internal.VehicleInternalApi;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Báo cáo doanh thu theo khách hàng và theo sản phẩm.
 *
 * Chọn hoá đơn y hệt trang Quản lý doanh thu ({@link RevenueReportService#collectBills}): hoá đơn
 * đã thu tính theo ngày thu, chưa thu tính theo ngày tiếp nhận phiếu, bỏ phiếu huỷ / xoá. Mỗi
 * hoá đơn được rã thành các dòng hàng của báo giá gắn với nó (dòng đã tick, chưa gỡ).
 *
 * Tổng dòng hàng thường bằng đúng tiền hoá đơn (hoá đơn lấy tổng báo giá), nhưng nếu lệch (giảm
 * giá cấp hoá đơn, dữ liệu cũ) thì phần lệch được chia theo tỉ lệ cho các dòng có tiền dương —
 * để cộng doanh thu theo sản phẩm luôn khớp với cộng theo khách và với trang Quản lý doanh thu.
 * Dòng thu mua linh kiện (TRADE_IN) giữ nguyên số âm, không nhận phần chia.
 */
@Service
@RequiredArgsConstructor
public class SalesReportService {

    /** Số dòng chi tiết trả về tối đa; tổng hợp theo khách / sản phẩm vẫn tính trên toàn bộ. */
    public static final int DETAIL_LIMIT = 5000;
    private static final int IN_CHUNK = 1000;

    private static final String TYPE_SERVICE = "SERVICE";
    private static final String TYPE_PART = "PART";
    private static final String TYPE_OTHER = "OTHER";
    private static final String TYPE_TRADE_IN = "TRADE_IN";
    private static final String TYPE_LEGACY = "LEGACY";
    private static final String TYPE_ADJUSTMENT = "ADJUSTMENT";

    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_UNPAID = "UNPAID";
    private static final String STATUS_ALL = "ALL";

    private static final String NO_CATEGORY = "Chưa phân loại";
    private static final String LEGACY_LABEL = "Sổ dịch vụ cũ";
    private static final String WALK_IN_LABEL = "Khách lẻ";

    private final RevenueReportService revenueReportService;
    private final LegacyVisitItemRepository legacyVisitItemRepository;
    private final CustomerInternalApi customerInternalApi;
    private final VehicleInternalApi vehicleInternalApi;
    private final EntityManager entityManager;

    /**
     * @param status      ALL (mặc định) | PAID | UNPAID — UNPAID gồm cả hoá đơn quá hạn
     * @param customerKey khoá khách lấy từ {@code customers[].customerKey}; null = mọi khách
     * @param productKey  khoá sản phẩm lấy từ {@code products[].productKey}; null = mọi sản phẩm
     * @param branchId    null = mọi xưởng
     */
    @Transactional(readOnly = true)
    public SalesReportResponse buildReport(LocalDate fromDate, LocalDate toDate, String status,
                                           String customerKey, String productKey,
                                           boolean includeLegacy, Integer branchId) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1);
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        if (from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }
        String statusFilter = normalizeStatus(status);
        String customerFilter = blankToNull(customerKey);
        String productFilter = blankToNull(productKey);

        // 1) Hoá đơn + lượt sổ cũ của kỳ.
        List<PeriodBill> bills = revenueReportService.collectBills(from, to, branchId).stream()
                .filter(b -> matchesStatus(b.status(), statusFilter))
                .toList();
        // Sổ cũ chỉ ghi lượt đã xong (coi là đã thu) nên lọc "chưa thu" thì không có.
        List<LegacyVisitJpa> legacyVisits = includeLegacy && !STATUS_UNPAID.equals(statusFilter)
                ? revenueReportService.collectLegacyVisits(from, to, branchId)
                : List.of();

        // 2) Tra cứu hàng loạt.
        Map<Integer, CustomerProfile> customerMap = loadCustomers(Stream.concat(
                bills.stream().map(b -> b.ticket().getCustomerId()),
                legacyVisits.stream().map(LegacyVisitJpa::getCustomerId)));
        Map<Integer, Vehicle> vehicleMap = loadVehicles(Stream.concat(
                bills.stream().map(b -> b.ticket().getVehicleId()),
                legacyVisits.stream().map(LegacyVisitJpa::getVehicleId)));
        Map<Integer, List<RawItem>> itemsByEstimate = loadEstimateItems(bills.stream()
                .map(b -> b.bill().getEstimateId()).filter(Objects::nonNull).distinct().toList());
        Map<Integer, List<LegacyVisitItemJpa>> itemsByVisit = loadLegacyItems(legacyVisits.stream()
                .map(LegacyVisitJpa::getLegacyVisitId).filter(Objects::nonNull).distinct().toList());

        // 3) Rã thành dòng.
        List<Fact> facts = new ArrayList<>();
        for (PeriodBill pb : bills) {
            addBillFacts(facts, pb, customerMap, vehicleMap,
                    itemsByEstimate.getOrDefault(pb.bill().getEstimateId(), List.of()));
        }
        for (LegacyVisitJpa visit : legacyVisits) {
            addLegacyFacts(facts, visit, customerMap, vehicleMap,
                    itemsByVisit.getOrDefault(visit.getLegacyVisitId(), List.of()));
        }

        // 4) Lọc chéo: danh sách khách bỏ qua bộ lọc khách, danh sách sản phẩm bỏ qua bộ lọc sản phẩm.
        Predicate<Fact> byCustomer = f -> customerFilter == null || customerFilter.equals(f.customerKey);
        Predicate<Fact> byProduct = f -> productFilter == null || productFilter.equals(f.productKey);
        List<Fact> customerScope = facts.stream().filter(byProduct).toList();
        List<Fact> productScope = facts.stream().filter(byCustomer).toList();
        List<Fact> scope = facts.stream().filter(byCustomer.and(byProduct)).toList();

        List<Fact> detailFacts = scope.stream()
                .sorted(Comparator.comparing((Fact f) -> f.date, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(f -> f.ticketCode, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(f -> f.lineOrder))
                .toList();
        List<DetailRow> details = detailFacts.stream().limit(DETAIL_LIMIT).map(SalesReportService::toDetail).toList();

        Summary summary = summarize(scope, from, to, statusFilter, includeLegacy);
        summary.setCustomerKey(customerFilter);
        summary.setProductKey(productFilter);
        if (customerFilter != null) {
            facts.stream().filter(f -> customerFilter.equals(f.customerKey)).findFirst()
                    .ifPresent(f -> summary.setCustomerName(f.customerName));
        }
        if (productFilter != null) {
            facts.stream().filter(f -> productFilter.equals(f.productKey)).findFirst()
                    .ifPresent(f -> summary.setProductName(f.productName));
        }
        summary.setDetailCount(detailFacts.size());
        summary.setDetailLimit(DETAIL_LIMIT);
        summary.setDetailsTruncated(detailFacts.size() > DETAIL_LIMIT);

        return SalesReportResponse.builder()
                .summary(summary)
                .customers(aggregateCustomers(customerScope))
                .products(aggregateProducts(productScope))
                .details(details)
                .build();
    }

    // ------------------------------------------------------------------------------------------
    // Rã hoá đơn / lượt sổ cũ thành dòng

    private void addBillFacts(List<Fact> out, PeriodBill pb, Map<Integer, CustomerProfile> customerMap,
                              Map<Integer, Vehicle> vehicleMap, List<RawItem> items) {
        ServiceBillJpa bill = pb.bill();
        ServiceTicketJpa ticket = pb.ticket();
        Vehicle vehicle = ticket.getVehicleId() != null ? vehicleMap.get(ticket.getVehicleId()) : null;

        Fact base = new Fact();
        base.invoiceKey = "B-" + bill.getBillId();
        base.date = pb.date();
        base.serviceTicketId = ticket.getServiceTicketId();
        base.ticketCode = nz(ticket.getTicketCode());
        base.ticketType = ticket.getTicketType() == TicketType.PARTS_SALE ? TYPE_PART : TYPE_SERVICE;
        base.status = pb.status();
        base.licensePlate = vehicle != null ? nz(vehicle.getLicensePlate()) : "";
        applyTicketCustomer(base, ticket, customerMap);

        List<Fact> lines = new ArrayList<>(items.size());
        int order = 0;
        for (RawItem item : items) {
            Fact f = base.copy();
            f.lineOrder = order++;
            resolveProduct(f, item);
            f.quantity = nvl(item.quantity);
            f.unitPrice = nvl(item.unitPrice);
            f.lineAmount = nvl(item.finalPrice);
            f.discountAmount = nvl(item.discount);
            f.gift = item.gift;
            lines.add(f);
        }
        distribute(out, base, lines, nvl(bill.getFinalAmount()), false);
    }

    private void addLegacyFacts(List<Fact> out, LegacyVisitJpa visit, Map<Integer, CustomerProfile> customerMap,
                                Map<Integer, Vehicle> vehicleMap, List<LegacyVisitItemJpa> items) {
        CustomerProfile customer = visit.getCustomerId() != null ? customerMap.get(visit.getCustomerId()) : null;
        Vehicle vehicle = visit.getVehicleId() != null ? vehicleMap.get(visit.getVehicleId()) : null;
        String code = nz(visit.getLegacyTicketCode()).trim();

        Fact base = new Fact();
        base.invoiceKey = "L-" + visit.getLegacyVisitId();
        base.date = visit.getVisitedAt() != null ? visit.getVisitedAt().toLocalDate() : null;
        base.legacyVisitId = visit.getLegacyVisitId();
        base.ticketCode = !code.isEmpty() ? code : "SC-" + visit.getLegacyVisitId();
        base.ticketType = TYPE_LEGACY;
        base.status = STATUS_PAID;
        base.legacy = true;
        base.licensePlate = vehicle != null ? nz(vehicle.getLicensePlate()) : "";
        applyProfileCustomer(base, visit.getCustomerId(), customer);

        List<Fact> lines = new ArrayList<>(items.size());
        int order = 0;
        for (LegacyVisitItemJpa item : items) {
            String name = firstNonBlank(item.getItemName(), item.getCategory(), "(Không ghi tên)");
            Fact f = base.copy();
            f.lineOrder = order++;
            f.productKey = "l:" + normalizeKey(name);
            f.productName = name.trim();
            f.sku = "";
            f.productType = TYPE_LEGACY;
            f.categoryName = firstNonBlank(item.getCategory(), LEGACY_LABEL, LEGACY_LABEL).trim();
            f.unit = "";
            f.quantity = nvl(item.getQuantity());
            f.unitPrice = nvl(item.getUnitPrice());
            f.lineAmount = nvl(item.getAmount());
            f.discountAmount = BigDecimal.ZERO;
            lines.add(f);
        }
        distribute(out, base, lines, nvl(visit.getTotalAmount()), true);
    }

    /**
     * Cân tiền các dòng cho khớp {@code total}: phần lệch chia theo tỉ lệ cho dòng có tiền dương
     * (dòng cuối nhận phần dư làm tròn). Không có dòng dương nào thì phần lệch thành một dòng
     * "điều chỉnh" riêng để tổng vẫn khớp.
     */
    private static void distribute(List<Fact> out, Fact base, List<Fact> lines, BigDecimal total, boolean legacy) {
        BigDecimal lineSum = BigDecimal.ZERO;
        BigDecimal positiveSum = BigDecimal.ZERO;
        int lastPositive = -1;
        for (int i = 0; i < lines.size(); i++) {
            BigDecimal amount = lines.get(i).lineAmount;
            lineSum = lineSum.add(amount);
            if (amount.signum() > 0 && !TYPE_TRADE_IN.equals(lines.get(i).productType)) {
                positiveSum = positiveSum.add(amount);
                lastPositive = i;
            }
        }
        BigDecimal diff = total.subtract(lineSum);

        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < lines.size(); i++) {
            Fact f = lines.get(i);
            BigDecimal adjustment = BigDecimal.ZERO;
            if (diff.signum() != 0 && positiveSum.signum() > 0
                    && f.lineAmount.signum() > 0 && !TYPE_TRADE_IN.equals(f.productType)) {
                adjustment = i == lastPositive
                        ? diff.subtract(allocated)
                        : diff.multiply(f.lineAmount).divide(positiveSum, 0, RoundingMode.HALF_UP);
                allocated = allocated.add(adjustment);
            }
            f.adjustment = adjustment;
            f.revenue = f.lineAmount.add(adjustment);
            out.add(f);
        }

        if (diff.signum() != 0 && positiveSum.signum() <= 0) {
            Fact f = base.copy();
            f.lineOrder = lines.size();
            boolean noLines = lines.isEmpty();
            if (legacy) {
                f.productKey = noLines ? "l:__none" : "l:__adjust";
                f.productName = noLines ? "Sổ cũ — không có chi tiết dòng" : "Sổ cũ — chênh lệch tổng tiền";
                f.categoryName = LEGACY_LABEL;
                f.productType = TYPE_LEGACY;
            } else {
                f.productKey = noLines ? "x:none" : "x:adjust";
                f.productName = noLines ? "Hoá đơn không có chi tiết hàng" : "Điều chỉnh tổng hoá đơn";
                f.categoryName = NO_CATEGORY;
                f.productType = TYPE_ADJUSTMENT;
            }
            f.sku = "";
            f.unit = "";
            f.quantity = BigDecimal.ZERO;
            f.unitPrice = BigDecimal.ZERO;
            f.lineAmount = BigDecimal.ZERO;
            f.discountAmount = BigDecimal.ZERO;
            f.adjustment = diff;
            f.revenue = diff;
            out.add(f);
        }
    }

    private static void resolveProduct(Fact f, RawItem item) {
        String lineName = nz(item.lineName).trim();
        f.categoryName = firstNonBlank(item.categoryName, item.categoryLabel, NO_CATEGORY).trim();
        if (TYPE_TRADE_IN.equalsIgnoreCase(nz(item.lineType).trim())) {
            String name = !lineName.isEmpty() ? lineName : "Thu mua linh kiện";
            f.productKey = "t:" + normalizeKey(name);
            f.productName = name;
            f.sku = "";
            f.productType = TYPE_TRADE_IN;
            f.unit = nz(item.unit).trim();
            return;
        }
        String type = itemType(nz(item.itemType), nz(item.categoryType));
        if (item.itemId != null) {
            f.productKey = "i:" + item.itemId;
            f.itemId = item.itemId;
            f.productName = firstNonBlank(item.catalogName, lineName, "Mặt hàng #" + item.itemId).trim();
            f.sku = nz(item.sku).trim();
            f.productType = type;
            f.unit = firstNonBlank(item.catalogUnit, item.unit, "").trim();
            return;
        }
        String name = !lineName.isEmpty() ? lineName : "(Dòng không tên)";
        f.productKey = "n:" + normalizeKey(name);
        f.productName = name;
        f.sku = "";
        f.productType = type;
        f.unit = nz(item.unit).trim();
    }

    /** Phiếu khách lẻ (bán vãng lai) gắn hồ sơ dùng chung, tên / SĐT thật nằm trên phiếu. */
    private static void applyTicketCustomer(Fact f, ServiceTicketJpa ticket, Map<Integer, CustomerProfile> customerMap) {
        if (Boolean.TRUE.equals(ticket.getIsWalkIn())) {
            String phone = digits(ticket.getWalkInPhone());
            f.walkIn = true;
            f.customerId = null;
            f.customerKey = !phone.isEmpty() ? "w:" + phone : "wt:" + ticket.getServiceTicketId();
            f.customerName = firstNonBlank(ticket.getWalkInName(), WALK_IN_LABEL, WALK_IN_LABEL).trim();
            f.customerPhone = phone;
            return;
        }
        Integer customerId = ticket.getCustomerId();
        applyProfileCustomer(f, customerId, customerId != null ? customerMap.get(customerId) : null);
    }

    private static void applyProfileCustomer(Fact f, Integer customerId, CustomerProfile customer) {
        f.walkIn = false;
        if (customerId == null) {
            f.customerKey = "c:none";
            f.customerName = WALK_IN_LABEL;
            f.customerPhone = "";
            return;
        }
        f.customerId = customerId;
        f.customerKey = "c:" + customerId;
        f.customerName = customer != null && !nz(customer.getFullName()).isBlank()
                ? customer.getFullName().trim() : "Khách #" + customerId;
        f.customerPhone = customer != null ? nz(customer.getPhone()).replaceAll("\\s+", "") : "";
    }

    // ------------------------------------------------------------------------------------------
    // Gom nhóm

    private static Summary summarize(List<Fact> facts, LocalDate from, LocalDate to, String status,
                                     boolean includeLegacy) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal tradeIn = BigDecimal.ZERO;
        BigDecimal adjustment = BigDecimal.ZERO;
        BigDecimal legacy = BigDecimal.ZERO;
        BigDecimal quantity = BigDecimal.ZERO;
        Set<String> invoices = new HashSet<>();
        Set<String> customers = new HashSet<>();
        Set<String> products = new HashSet<>();
        for (Fact f : facts) {
            total = total.add(f.revenue);
            if (STATUS_PAID.equals(f.status)) {
                paid = paid.add(f.revenue);
            }
            if (TYPE_TRADE_IN.equals(f.productType)) {
                tradeIn = tradeIn.add(f.revenue);
            } else if (!TYPE_ADJUSTMENT.equals(f.productType)) {
                quantity = quantity.add(f.quantity);
            }
            if (f.legacy) {
                legacy = legacy.add(f.revenue);
            }
            adjustment = adjustment.add(f.adjustment);
            invoices.add(f.invoiceKey);
            customers.add(f.customerKey);
            products.add(f.productKey);
        }
        return Summary.builder()
                .from(from)
                .to(to)
                .status(status)
                .legacyIncluded(includeLegacy)
                .totalRevenue(total)
                .paidRevenue(paid)
                .unpaidRevenue(total.subtract(paid))
                .tradeInAmount(tradeIn)
                .billAdjustment(adjustment)
                .legacyRevenue(legacy)
                .quantity(quantity)
                .invoiceCount(invoices.size())
                .customerCount(customers.size())
                .productCount(products.size())
                .build();
    }

    private static List<CustomerRow> aggregateCustomers(List<Fact> facts) {
        Map<String, CustomerAcc> acc = new LinkedHashMap<>();
        for (Fact f : facts) {
            CustomerAcc a = acc.computeIfAbsent(f.customerKey, k -> new CustomerAcc(f));
            a.invoices.add(f.invoiceKey);
            if (STATUS_PAID.equals(f.status)) {
                a.paidInvoices.add(f.invoiceKey);
                a.paid = a.paid.add(f.revenue);
            }
            a.products.add(f.productKey);
            a.revenue = a.revenue.add(f.revenue);
            if (f.legacy) {
                a.legacy = a.legacy.add(f.revenue);
            }
            if (!f.licensePlate.isBlank()) {
                a.plates.add(f.licensePlate.trim());
            }
            if (a.customerPhone.isEmpty() && !f.customerPhone.isEmpty()) {
                a.customerPhone = f.customerPhone;
            }
            if (f.date != null) {
                a.firstDate = a.firstDate == null || f.date.isBefore(a.firstDate) ? f.date : a.firstDate;
                a.lastDate = a.lastDate == null || f.date.isAfter(a.lastDate) ? f.date : a.lastDate;
            }
        }
        return acc.values().stream()
                .map(a -> CustomerRow.builder()
                        .customerKey(a.customerKey)
                        .customerId(a.customerId)
                        .customerName(a.customerName)
                        .customerPhone(a.customerPhone)
                        .walkIn(a.walkIn)
                        .plates(new ArrayList<>(a.plates))
                        .invoiceCount(a.invoices.size())
                        .paidInvoiceCount(a.paidInvoices.size())
                        .productCount(a.products.size())
                        .revenue(a.revenue)
                        .paidRevenue(a.paid)
                        .unpaidRevenue(a.revenue.subtract(a.paid))
                        .legacyRevenue(a.legacy)
                        .firstDate(a.firstDate)
                        .lastDate(a.lastDate)
                        .build())
                .sorted(Comparator.comparing(CustomerRow::getRevenue, Comparator.reverseOrder())
                        .thenComparing(CustomerRow::getInvoiceCount, Comparator.reverseOrder())
                        .thenComparing(CustomerRow::getCustomerName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static List<ProductRow> aggregateProducts(List<Fact> facts) {
        Map<String, ProductAcc> acc = new LinkedHashMap<>();
        for (Fact f : facts) {
            ProductAcc a = acc.computeIfAbsent(f.productKey, k -> new ProductAcc(f));
            a.quantity = a.quantity.add(f.quantity);
            a.lineAmount = a.lineAmount.add(f.lineAmount);
            a.discount = a.discount.add(f.discountAmount);
            a.revenue = a.revenue.add(f.revenue);
            if (STATUS_PAID.equals(f.status)) {
                a.paid = a.paid.add(f.revenue);
            }
            a.invoices.add(f.invoiceKey);
            a.customers.add(f.customerKey);
            if (f.gift) {
                a.giftCount++;
            }
            if (a.unit.isEmpty() && !f.unit.isEmpty()) {
                a.unit = f.unit;
            }
            if (f.date != null && (a.lastDate == null || f.date.isAfter(a.lastDate))) {
                a.lastDate = f.date;
            }
        }
        return acc.values().stream()
                .map(a -> ProductRow.builder()
                        .productKey(a.productKey)
                        .itemId(a.itemId)
                        .sku(a.sku)
                        .productName(a.productName)
                        .productType(a.productType)
                        .categoryName(a.categoryName)
                        .unit(a.unit)
                        .quantity(a.quantity)
                        .lineAmount(a.lineAmount)
                        .discountAmount(a.discount)
                        .revenue(a.revenue)
                        .paidRevenue(a.paid)
                        .unpaidRevenue(a.revenue.subtract(a.paid))
                        .invoiceCount(a.invoices.size())
                        .customerCount(a.customers.size())
                        .giftCount(a.giftCount)
                        .lastDate(a.lastDate)
                        .build())
                .sorted(Comparator.comparing(ProductRow::getRevenue, Comparator.reverseOrder())
                        .thenComparing(ProductRow::getQuantity, Comparator.reverseOrder())
                        .thenComparing(ProductRow::getProductName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static DetailRow toDetail(Fact f) {
        return DetailRow.builder()
                .date(f.date)
                .serviceTicketId(f.serviceTicketId)
                .legacyVisitId(f.legacyVisitId)
                .ticketCode(f.ticketCode)
                .ticketType(f.ticketType)
                .status(f.status)
                .customerKey(f.customerKey)
                .customerName(f.customerName)
                .customerPhone(f.customerPhone)
                .licensePlate(f.licensePlate)
                .productKey(f.productKey)
                .productName(f.productName)
                .sku(f.sku)
                .productType(f.productType)
                .unit(f.unit)
                .quantity(f.quantity)
                .unitPrice(f.unitPrice)
                .lineAmount(f.lineAmount)
                .discountAmount(f.discountAmount)
                .revenue(f.revenue)
                .gift(f.gift)
                .build();
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

    /** Dòng báo giá được chốt (đã tick, chưa gỡ) kèm thông tin mặt hàng và danh mục. */
    private Map<Integer, List<RawItem>> loadEstimateItems(List<Integer> estimateIds) {
        Map<Integer, List<RawItem>> result = new HashMap<>();
        for (List<Integer> chunk : chunks(estimateIds)) {
            List<?> rows = entityManager.createNativeQuery("""
                            SELECT ei.estimate_id, ei.item_id, ei.item_name, ei.quantity, ei.unit_price,
                                   ei.final_price, ei.discount_amount, ei.unit, ei.line_type, ei.is_gift,
                                   ei.category_label, ci.item_name, ci.sku, ci.item_type, ci.unit,
                                   ic.category_name, ic.category_type
                            FROM estimate_item ei
                            LEFT JOIN catalog_item ci ON ci.item_id = ei.item_id
                            LEFT JOIN item_category ic
                                   ON ic.item_category_id = COALESCE(ei.item_category_id, ci.item_category_id)
                            WHERE ei.estimate_id IN (:ids)
                              AND COALESCE(ei.is_checked, 0) = 1
                              AND COALESCE(ei.is_removed, 0) = 0
                            ORDER BY ei.estimate_id, ei.estimate_item_id
                            """)
                    .setParameter("ids", chunk)
                    .getResultList();
            for (Object row : rows) {
                Object[] r = (Object[]) row;
                RawItem item = new RawItem(
                        r[1] != null ? toInt(r[1]) : null,
                        str(r[2]), toDecimal(r[3]), toDecimal(r[4]), toDecimal(r[5]), toDecimal(r[6]),
                        str(r[7]), str(r[8]), toBool(r[9]), str(r[10]),
                        str(r[11]), str(r[12]), str(r[13]), str(r[14]), str(r[15]), str(r[16]));
                result.computeIfAbsent(toInt(r[0]), k -> new ArrayList<>()).add(item);
            }
        }
        return result;
    }

    private Map<Integer, List<LegacyVisitItemJpa>> loadLegacyItems(List<Integer> visitIds) {
        Map<Integer, List<LegacyVisitItemJpa>> result = new HashMap<>();
        for (List<Integer> chunk : chunks(visitIds)) {
            for (LegacyVisitItemJpa item : legacyVisitItemRepository.findByLegacyVisitIdInOrderByLineNoAsc(chunk)) {
                result.computeIfAbsent(item.getLegacyVisitId(), k -> new ArrayList<>()).add(item);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------------------------------
    // Tiện ích

    private static String normalizeStatus(String status) {
        String s = nz(status).trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case STATUS_PAID -> STATUS_PAID;
            case STATUS_UNPAID, "OVERDUE" -> STATUS_UNPAID;
            default -> STATUS_ALL;
        };
    }

    private static boolean matchesStatus(String billStatus, String filter) {
        if (STATUS_PAID.equals(filter)) {
            return STATUS_PAID.equals(billStatus);
        }
        if (STATUS_UNPAID.equals(filter)) {
            return !STATUS_PAID.equals(billStatus);
        }
        return true;
    }

    /** Loại hàng theo catalog_item.item_type, thiếu thì đoán theo item_category.category_type. */
    private static String itemType(String itemType, String categoryType) {
        switch (itemType.trim().toUpperCase(Locale.ROOT)) {
            case "SERVICE", "COMBO", "MAINTENANCE_PACKAGE":
                return TYPE_SERVICE;
            case "PART", "EQUIPMENT", "MACHINERY":
                return TYPE_PART;
            default:
                break;
        }
        String ct = categoryType.trim().toUpperCase(Locale.ROOT);
        if (ct.startsWith("SERV")) {
            return TYPE_SERVICE;
        }
        if (ct.startsWith("PART")) {
            return TYPE_PART;
        }
        return TYPE_OTHER;
    }

    /** Khoá gộp cho dòng không gắn mặt hàng: chữ thường, gộp khoảng trắng. */
    private static String normalizeKey(String name) {
        return nz(name).trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static <T> List<List<T>> chunks(List<T> list) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += IN_CHUNK) {
            result.add(list.subList(i, Math.min(list.size(), i + IN_CHUNK)));
        }
        return result;
    }

    private static String firstNonBlank(String a, String b, String fallback) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return fallback;
    }

    private static String digits(String value) {
        return nz(value).replaceAll("[^0-9+]", "");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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

    private static boolean toBool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        return false;
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

    // ------------------------------------------------------------------------------------------

    private record RawItem(Integer itemId, String lineName, BigDecimal quantity, BigDecimal unitPrice,
                           BigDecimal finalPrice, BigDecimal discount, String unit, String lineType,
                           boolean gift, String categoryLabel, String catalogName, String sku,
                           String itemType, String catalogUnit, String categoryName, String categoryType) {
    }

    /** Một dòng hàng của một hoá đơn / lượt sổ cũ, đã gắn khách và sản phẩm. */
    private static final class Fact {
        String invoiceKey;
        LocalDate date;
        Integer serviceTicketId;
        Integer legacyVisitId;
        String ticketCode = "";
        String ticketType = "";
        String status = "";
        boolean legacy;
        int lineOrder;

        String customerKey = "";
        Integer customerId;
        String customerName = "";
        String customerPhone = "";
        boolean walkIn;
        String licensePlate = "";

        String productKey = "";
        Integer itemId;
        String sku = "";
        String productName = "";
        String productType = TYPE_OTHER;
        String categoryName = NO_CATEGORY;
        String unit = "";
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal unitPrice = BigDecimal.ZERO;
        BigDecimal lineAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal adjustment = BigDecimal.ZERO;
        BigDecimal revenue = BigDecimal.ZERO;
        boolean gift;

        /** Bản sao phần hoá đơn + khách; phần sản phẩm để mặc định cho dòng mới tự điền. */
        Fact copy() {
            Fact f = new Fact();
            f.invoiceKey = invoiceKey;
            f.date = date;
            f.serviceTicketId = serviceTicketId;
            f.legacyVisitId = legacyVisitId;
            f.ticketCode = ticketCode;
            f.ticketType = ticketType;
            f.status = status;
            f.legacy = legacy;
            f.customerKey = customerKey;
            f.customerId = customerId;
            f.customerName = customerName;
            f.customerPhone = customerPhone;
            f.walkIn = walkIn;
            f.licensePlate = licensePlate;
            return f;
        }
    }

    private static final class CustomerAcc {
        final String customerKey;
        final Integer customerId;
        final String customerName;
        final boolean walkIn;
        String customerPhone;
        final Set<String> plates = new LinkedHashSet<>();
        final Set<String> invoices = new HashSet<>();
        final Set<String> paidInvoices = new HashSet<>();
        final Set<String> products = new HashSet<>();
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal legacy = BigDecimal.ZERO;
        LocalDate firstDate;
        LocalDate lastDate;

        CustomerAcc(Fact f) {
            this.customerKey = f.customerKey;
            this.customerId = f.customerId;
            this.customerName = f.customerName;
            this.walkIn = f.walkIn;
            this.customerPhone = f.customerPhone;
        }
    }

    private static final class ProductAcc {
        final String productKey;
        final Integer itemId;
        final String sku;
        final String productName;
        final String productType;
        final String categoryName;
        String unit;
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal lineAmount = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        final Set<String> invoices = new HashSet<>();
        final Set<String> customers = new HashSet<>();
        long giftCount;
        LocalDate lastDate;

        ProductAcc(Fact f) {
            this.productKey = f.productKey;
            this.itemId = f.itemId;
            this.sku = f.sku;
            this.productName = f.productName;
            this.productType = f.productType;
            this.categoryName = f.categoryName;
            this.unit = f.unit;
        }
    }
}
