package com.g42.platform.gms.customercare.application.service;

import com.g42.platform.gms.customercare.api.dto.CareDtos.*;
import com.g42.platform.gms.customercare.domain.CareCallOutcome;
import com.g42.platform.gms.customercare.domain.CareStatus;
import com.g42.platform.gms.customercare.domain.LegacyCallNoteClassifier;
import com.g42.platform.gms.customercare.infrastructure.entity.CustomerCareCallJpa;
import com.g42.platform.gms.customercare.infrastructure.entity.CustomerCareProfileJpa;
import com.g42.platform.gms.customercare.infrastructure.jdbc.CareDataReader;
import com.g42.platform.gms.customercare.infrastructure.jdbc.CareDataReader.*;
import com.g42.platform.gms.customercare.infrastructure.repository.CustomerCareCallRepository;
import com.g42.platform.gms.customercare.infrastructure.repository.CustomerCareProfileRepository;
import com.g42.platform.gms.customerimport.application.service.ImportNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Gọi chăm sóc khách hàng — bản phần mềm của file Excel "Tổng / Đã gọi / Lặp sđt / Gọi thành
 * công / Note / E-mail / Biển số / Hãng xe".
 *
 * Cột nào của file cũ đi đâu:
 *  - Họ tên, SĐT, E-mail, Biển số, Hãng xe → đọc thẳng từ danh bạ + xe, không chép.
 *  - Tổng → cộng hoá đơn đã thu + tiền sổ cũ, tính lúc đọc (file cũ có ô "#REF!").
 *  - Đã gọi / Gọi thành công / Note → nhật ký customer_care_call; trạng thái suy ra từ cuộc
 *    gọi gần nhất. Khách chưa có cuộc gọi nào trong phần mềm thì đọc kết quả gọi chép ở sổ cũ.
 *  - Lặp sđt → dò hồ sơ khác dùng chung SĐT hoặc biển số, gợi ý sang màn gộp hồ sơ.
 *
 * Cả danh sách tính trong bộ nhớ: danh bạ xưởng cỡ vài nghìn khách, mỗi nguồn một truy vấn
 * gom nhóm (CareDataReader), rồi lọc/sắp xếp/phân trang ở đây. Trạng thái là giá trị suy ra
 * nên không lọc được bằng SQL mà không lưu thêm cột — và cột lưu thêm thì sẽ lệch.
 */
@Service
@RequiredArgsConstructor
public class CustomerCareService {

    public static final String TAB_DUE = "DUE";
    public static final String TAB_ALL = "ALL";
    private static final String SOURCE_SYSTEM = "SYSTEM";
    private static final String SOURCE_LEGACY = "LEGACY";
    private static final int MAX_PAGE_SIZE = 10000;

    private final CareDataReader reader;
    private final CustomerCareCallRepository callRepo;
    private final CustomerCareProfileRepository profileRepo;

    /** Bộ lọc danh sách. Mọi trường có thể trống. */
    public record ListQuery(String tab, String search, Integer minDays, Integer maxDays, BigDecimal minSpend,
                            String source, String flag, String sort, boolean includeNoHistory,
                            int page, int size) {
    }

    // ================================================================ đọc

    public List<OutcomeOption> outcomes() {
        List<OutcomeOption> result = new ArrayList<>();
        for (CareCallOutcome o : CareCallOutcome.values()) {
            OutcomeOption opt = new OutcomeOption();
            opt.setCode(o.name());
            opt.setLabel(o.getLabel());
            opt.setReached(o.isReached());
            opt.setDefaultFollowUpDays(o.getDefaultFollowUpDays());
            opt.setStopsContact(o.isStopsContact());
            result.add(opt);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public CareListResponse list(ListQuery q) {
        LocalDate today = LocalDate.now();
        List<CareCustomerRow> rows = buildRows(null, today);

        // Khách chưa từng đến xưởng và chưa từng được gọi (VD tự đăng ký tài khoản online)
        // không có gì để chăm sóc — ẩn mặc định cho danh sách gọn như file cũ.
        Predicate<CareCustomerRow> base = row -> q.includeNoHistory() || row.getVisitCount() > 0 || row.getCallCount() > 0;
        base = base.and(searchFilter(q.search()))
                .and(daysFilter(q.minDays(), q.maxDays()))
                .and(spendFilter(q.minSpend()))
                .and(sourceFilter(q.source()))
                .and(flagFilter(q.flag()));

        List<CareCustomerRow> filtered = rows.stream().filter(base).toList();

        CareListResponse response = new CareListResponse();
        response.setCounts(countTabs(filtered));

        String tab = q.tab() == null || q.tab().isBlank() ? TAB_DUE : q.tab().trim().toUpperCase();
        List<CareCustomerRow> inTab = new ArrayList<>(filtered.stream().filter(tabFilter(tab)).toList());
        inTab.sort(comparator(q.sort(), tab));

        response.setFilteredSpend(inTab.stream().map(CareCustomerRow::getTotalSpend)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        int size = Math.max(1, Math.min(q.size() <= 0 ? 20 : q.size(), MAX_PAGE_SIZE));
        int page = Math.max(0, q.page());
        int from = Math.min(page * size, inTab.size());
        int to = Math.min(from + size, inTab.size());
        response.setContent(new ArrayList<>(inTab.subList(from, to)));
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(inTab.size());
        response.setTotalPages(Math.max(1, (int) Math.ceil(inTab.size() / (double) size)));

        TodayRow todayRow = reader.today(today);
        TodayStats stats = new TodayStats();
        stats.setCalls(todayRow.calls());
        stats.setReached(todayRow.reached());
        stats.setBooked(todayRow.booked());
        response.setToday(stats);
        return response;
    }

    @Transactional(readOnly = true)
    public CareCustomerDetail detail(int customerId) {
        List<CareCustomerRow> rows = buildRows(customerId, LocalDate.now());
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng #" + customerId);
        }
        CareCustomerDetail detail = new CareCustomerDetail();
        detail.setCustomer(rows.get(0));

        Map<Integer, String> staffNames = reader.staffNames();
        List<CallEntry> calls = new ArrayList<>();
        for (CallRow row : reader.callsOf(customerId)) calls.add(toEntry(row, staffNames));
        for (LegacyCallRow row : reader.legacyCalls(customerId)) calls.add(toEntry(row));
        calls.sort(Comparator.comparing(CallEntry::getCalledAt, Comparator.nullsLast(Comparator.reverseOrder())));
        detail.setCalls(calls);
        return detail;
    }

    // ================================================================ ghi

    @Transactional
    public CareCustomerDetail logCall(int customerId, CallRequest req, Integer staffId) {
        requireCustomer(customerId);
        if (req == null) throw badRequest("Thiếu nội dung cuộc gọi.");

        CareCallOutcome outcome = CareCallOutcome.parse(req.getOutcome());
        if (outcome == null) throw badRequest("Chọn kết quả cuộc gọi.");

        String note = trimToNull(req.getNote());
        if (outcome == CareCallOutcome.OTHER && note == null) {
            throw badRequest("Chọn kết quả \"Khác\" thì ghi rõ ở phần ghi chú.");
        }
        if (note != null && note.length() > 1000) throw badRequest("Ghi chú tối đa 1000 ký tự.");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime calledAt = req.getCalledAt() != null ? req.getCalledAt() : now;
        if (calledAt.isAfter(now.plusMinutes(5))) throw badRequest("Thời điểm gọi không được ở tương lai.");

        LocalDate followUp = req.getFollowUpDate();
        if (followUp == null && Boolean.TRUE.equals(req.getAutoFollowUp()) && outcome.getDefaultFollowUpDays() != null) {
            followUp = calledAt.toLocalDate().plusDays(outcome.getDefaultFollowUpDays());
        }
        if (outcome.isStopsContact()) followUp = null;
        if (followUp != null && followUp.isBefore(calledAt.toLocalDate())) {
            throw badRequest("Ngày hẹn gọi lại phải từ ngày gọi trở đi.");
        }

        String phone = trimToNull(req.getPhone());
        if (phone != null && phone.length() > 20) phone = phone.substring(0, 20);

        CustomerCareCallJpa call = new CustomerCareCallJpa();
        call.setCustomerId(customerId);
        call.setPhone(phone);
        call.setCalledAt(calledAt);
        call.setStaffId(staffId);
        call.setReached(outcome.isReached());
        call.setOutcome(outcome.name());
        call.setNote(note);
        call.setFollowUpDate(followUp);
        callRepo.save(call);

        // Chặn số / sai số / khách xin thôi gọi: bật cờ chung với phân hệ nhắc bảo dưỡng để
        // khách rời khỏi MỌI danh sách gọi, không riêng màn này.
        if (outcome.isStopsContact()) reader.setDoNotContact(customerId, true);

        return detail(customerId);
    }

    /** Xoá một cuộc gọi ghi nhầm. Không đụng cờ ngừng liên hệ — muốn gỡ thì tắt riêng. */
    @Transactional
    public CareCustomerDetail deleteCall(int careCallId) {
        CustomerCareCallJpa call = callRepo.findById(careCallId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc gọi #" + careCallId));
        int customerId = call.getCustomerId();
        callRepo.delete(call);
        return detail(customerId);
    }

    @Transactional
    public CareCustomerDetail saveCareProfile(int customerId, CareProfileRequest req, Integer staffId) {
        requireCustomer(customerId);
        String careNote = trimToNull(req != null ? req.getCareNote() : null);
        String preferredTime = trimToNull(req != null ? req.getPreferredTime() : null);
        if (careNote != null && careNote.length() > 1000) throw badRequest("Ghi chú chăm sóc tối đa 1000 ký tự.");
        if (preferredTime != null && preferredTime.length() > 100) throw badRequest("Khung giờ gọi tối đa 100 ký tự.");

        CustomerCareProfileJpa profile = profileRepo.findById(customerId).orElseGet(() -> {
            CustomerCareProfileJpa created = new CustomerCareProfileJpa();
            created.setCustomerId(customerId);
            return created;
        });
        profile.setCareNote(careNote);
        profile.setPreferredTime(preferredTime);
        profile.setUpdatedBy(staffId);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepo.save(profile);
        return detail(customerId);
    }

    @Transactional
    public CareCustomerDetail setDoNotContact(int customerId, boolean value) {
        requireCustomer(customerId);
        reader.setDoNotContact(customerId, value);
        return detail(customerId);
    }

    // ================================================================ dựng dòng

    private List<CareCustomerRow> buildRows(Integer onlyCustomerId, LocalDate today) {
        Map<Integer, CustomerBase> customers = reader.customers(onlyCustomerId);
        if (customers.isEmpty()) return List.of();

        Map<Integer, List<String>> extraPhones = reader.extraPhones(onlyCustomerId);
        Map<Integer, List<VehicleBrief>> vehicles = reader.vehicles(onlyCustomerId);
        Map<Integer, VisitAgg> systemVisits = reader.systemVisits(onlyCustomerId);
        Map<Integer, BigDecimal> systemSpend = reader.systemSpend(onlyCustomerId);
        Map<Integer, VisitAgg> legacyVisits = reader.legacyVisits(onlyCustomerId);
        Map<Integer, Integer> callCounts = reader.callCounts(onlyCustomerId);
        Map<Integer, CallRow> latestCalls = reader.latestCalls(onlyCustomerId);
        Map<Integer, CareProfileRow> profiles = reader.careProfiles(onlyCustomerId);
        Map<Integer, String> staffNames = reader.staffNames();

        // Sổ cũ: giữ lượt mới nhất có ghi kết quả gọi + đếm số lượt đó
        Map<Integer, LegacyCallRow> latestLegacyCall = new HashMap<>();
        Map<Integer, Integer> legacyCallCounts = new HashMap<>();
        for (LegacyCallRow row : reader.legacyCalls(onlyCustomerId)) {
            latestLegacyCall.putIfAbsent(row.customerId(), row); // truy vấn đã sắp mới nhất trước
            legacyCallCounts.merge(row.customerId(), 1, Integer::sum);
        }

        DuplicateIndex duplicates = buildDuplicateIndex(customers);

        List<CareCustomerRow> result = new ArrayList<>(customers.size());
        for (CustomerBase c : customers.values()) {
            CareCustomerRow row = new CareCustomerRow();
            row.setCustomerId(c.customerId());
            row.setCustomerCode(c.customerCode());
            row.setFullName(trimToNull(c.fullName()));
            row.setEmail(trimToNull(c.email()));
            row.setDoNotContact(c.doNotContact());

            // SĐT: số chính trước; không có thì đẩy số phụ đầu tiên lên làm số để gọi
            List<String> phones = new ArrayList<>();
            if (trimToNull(c.phone()) != null) phones.add(c.phone().trim());
            for (String p : extraPhones.getOrDefault(c.customerId(), List.of())) {
                String trimmed = trimToNull(p);
                if (trimmed != null && !phones.contains(trimmed)) phones.add(trimmed);
            }
            row.setPhone(phones.isEmpty() ? null : phones.get(0));
            row.setExtraPhones(phones.size() > 1 ? new ArrayList<>(phones.subList(1, phones.size())) : new ArrayList<>());
            row.setPhoneValid(isCallable(row.getPhone()));

            row.setVehicles(vehicles.getOrDefault(c.customerId(), new ArrayList<>()));

            // Lượt đến + tiền
            VisitAgg sys = systemVisits.get(c.customerId());
            VisitAgg leg = legacyVisits.get(c.customerId());
            row.setSystemVisitCount(sys != null ? sys.count() : 0);
            row.setLegacyVisitCount(leg != null ? leg.count() : 0);
            row.setVisitCount(row.getSystemVisitCount() + row.getLegacyVisitCount());
            row.setSystemSpend(systemSpend.getOrDefault(c.customerId(), BigDecimal.ZERO));
            row.setLegacySpend(leg != null ? leg.spend() : BigDecimal.ZERO);
            row.setTotalSpend(row.getSystemSpend().add(row.getLegacySpend()));
            LocalDateTime sysLast = sys != null ? sys.lastAt() : null;
            LocalDateTime legLast = leg != null ? leg.lastAt() : null;
            if (sysLast != null && (legLast == null || !legLast.isAfter(sysLast))) {
                row.setLastVisitAt(sysLast);
                row.setLastVisitSource(SOURCE_SYSTEM);
            } else if (legLast != null) {
                row.setLastVisitAt(legLast);
                row.setLastVisitSource(SOURCE_LEGACY);
            }
            if (row.getLastVisitAt() != null) {
                row.setDaysSinceLastVisit((int) ChronoUnit.DAYS.between(row.getLastVisitAt().toLocalDate(), today));
            }

            // Cuộc gọi gần nhất: trong phần mềm, hoặc kết quả chép ở sổ cũ nếu mới hơn
            CallRow sysCall = latestCalls.get(c.customerId());
            LegacyCallRow legCall = latestLegacyCall.get(c.customerId());
            CallEntry last = sysCall != null ? toEntry(sysCall, staffNames) : null;
            if (legCall != null && (last == null || (legCall.visitedAt() != null && last.getCalledAt() != null
                    && legCall.visitedAt().isAfter(last.getCalledAt())))) {
                last = toEntry(legCall);
            }
            row.setLastCall(last);
            row.setCallCount(callCounts.getOrDefault(c.customerId(), 0)
                    + legacyCallCounts.getOrDefault(c.customerId(), 0));

            CareProfileRow profile = profiles.get(c.customerId());
            if (profile != null) {
                row.setCareNote(profile.careNote());
                row.setPreferredTime(profile.preferredTime());
            }

            row.setDuplicates(duplicates.hintsFor(c.customerId(), phones, row.getVehicles(), customers));

            deriveStatus(row, today);
            result.add(row);
        }

        // Màn chi tiết chỉ nạp một khách nên chưa có tên của hồ sơ trùng — lấy bổ sung
        if (onlyCustomerId != null) {
            Set<Integer> missing = result.stream().flatMap(r -> r.getDuplicates().stream())
                    .filter(h -> h.getFullName() == null).map(DuplicateHint::getCustomerId)
                    .collect(Collectors.toSet());
            if (!missing.isEmpty()) {
                Map<Integer, String> names = reader.namesOf(missing);
                result.forEach(r -> r.getDuplicates().forEach(h -> {
                    if (h.getFullName() == null) h.setFullName(names.get(h.getCustomerId()));
                }));
            }
        }
        return result;
    }

    /**
     * Trạng thái suy ra — thứ tự các nhánh là quy tắc nghiệp vụ:
     *  1. Đã bật ngừng liên hệ → STOPPED, bất kể lịch sử gọi.
     *  2. Không có số nào → NO_PHONE (file cũ có nhiều dòng trống SĐT, chỉ có biển số).
     *  3. Chưa gọi lần nào → NOT_CALLED.
     *  4. Khách đã đến xưởng SAU cuộc gọi gần nhất → vòng chăm sóc mới, lại NOT_CALLED
     *     (kèm cờ returnedAfterCall để thấy cuộc gọi trước có tác dụng).
     *  5. Còn lại theo cuộc gọi gần nhất: không liên lạc được → NO_ANSWER; đã hẹn lịch →
     *     BOOKED; có ngày hẹn gọi lại → FOLLOW_UP; từ chối → DECLINED; khác → CONTACTED.
     * Đến lượt gọi (due): NOT_CALLED luôn đến lượt; NO_ANSWER / FOLLOW_UP khi chưa đặt ngày
     * hoặc đã tới ngày hẹn.
     */
    private void deriveStatus(CareCustomerRow row, LocalDate today) {
        CallEntry last = row.getLastCall();
        CareStatus status;
        boolean returned = false;

        if (row.isDoNotContact()) {
            status = CareStatus.STOPPED;
        } else if (row.getPhone() == null) {
            status = CareStatus.NO_PHONE;
        } else if (last == null) {
            status = CareStatus.NOT_CALLED;
        } else if (row.getLastVisitAt() != null && last.getCalledAt() != null
                && row.getLastVisitAt().isAfter(last.getCalledAt())) {
            status = CareStatus.NOT_CALLED;
            returned = true;
        } else {
            CareCallOutcome outcome = CareCallOutcome.parse(last.getOutcome());
            if (!last.isReached()) {
                status = CareStatus.NO_ANSWER;
            } else if (outcome == CareCallOutcome.BOOKED) {
                status = CareStatus.BOOKED;
            } else if (last.getFollowUpDate() != null) {
                status = CareStatus.FOLLOW_UP;
            } else if (outcome == CareCallOutcome.DECLINED) {
                status = CareStatus.DECLINED;
            } else {
                status = CareStatus.CONTACTED;
            }
        }

        boolean due = switch (status) {
            case NOT_CALLED -> true;
            case NO_ANSWER, FOLLOW_UP -> last == null || last.getFollowUpDate() == null
                    || !last.getFollowUpDate().isAfter(today);
            default -> false;
        };

        row.setStatus(status.name());
        row.setStatusLabel(status.getLabel());
        row.setDue(due);
        row.setReturnedAfterCall(returned);
        row.setFollowUpDate(!returned && last != null
                && (status == CareStatus.NO_ANSWER || status == CareStatus.FOLLOW_UP)
                ? last.getFollowUpDate() : null);
    }

    private CallEntry toEntry(CallRow row, Map<Integer, String> staffNames) {
        CallEntry e = new CallEntry();
        CareCallOutcome outcome = CareCallOutcome.parse(row.outcome());
        e.setCareCallId(row.careCallId());
        e.setSource(SOURCE_SYSTEM);
        e.setCalledAt(row.calledAt());
        e.setPhone(row.phone());
        e.setStaffId(row.staffId());
        e.setStaffName(row.staffId() != null ? staffNames.get(row.staffId()) : null);
        e.setReached(row.reached());
        e.setOutcome(row.outcome());
        e.setOutcomeLabel(outcome != null ? outcome.getLabel() : row.outcome());
        e.setNote(row.note());
        e.setFollowUpDate(row.followUpDate());
        return e;
    }

    /** Kết quả gọi ở sổ cũ: sổ không ghi ngày gọi nên mượn ngày của lượt dịch vụ đó. */
    private CallEntry toEntry(LegacyCallRow row) {
        CareCallOutcome outcome = LegacyCallNoteClassifier.classify(row.note(), row.callSuccess());
        CallEntry e = new CallEntry();
        e.setSource(SOURCE_LEGACY);
        e.setLegacyVisitId(row.legacyVisitId());
        e.setCalledAt(row.visitedAt());
        e.setDateApproximate(true);
        e.setReached(outcome == CareCallOutcome.OTHER ? row.callSuccess() : outcome.isReached());
        e.setOutcome(outcome.name());
        e.setOutcomeLabel(outcome.getLabel());
        e.setNote(trimToNull(row.note()));
        return e;
    }

    // ================================================================ trùng hồ sơ

    /** Chỉ mục SĐT chuẩn hoá / biển số → các hồ sơ đang dùng, chỉ giữ khoá có từ 2 hồ sơ trở lên. */
    private record DuplicateIndex(Map<String, Set<Integer>> phones, Map<String, Set<Integer>> plates) {

        List<DuplicateHint> hintsFor(int customerId, List<String> ownPhones, List<VehicleBrief> ownVehicles,
                                     Map<Integer, CustomerBase> customers) {
            Map<Integer, DuplicateHint> hints = new LinkedHashMap<>();
            for (String p : ownPhones) {
                String key = ImportNormalizer.normalizePhone(p);
                for (Integer other : phones.getOrDefault(key, Set.of())) {
                    if (other == customerId) continue;
                    hints.computeIfAbsent(other, id -> hint(id, customers, "Cùng SĐT " + key));
                }
            }
            for (VehicleBrief v : ownVehicles) {
                String key = com.g42.platform.gms.vehicle.support.PlateKeys.normalize(v.getLicensePlate());
                for (Integer other : plates.getOrDefault(key, Set.of())) {
                    if (other == customerId) continue;
                    hints.computeIfAbsent(other, id -> hint(id, customers, "Cùng biển số " + v.getLicensePlate()));
                }
            }
            return new ArrayList<>(hints.values());
        }

        private static DuplicateHint hint(int id, Map<Integer, CustomerBase> customers, String reason) {
            DuplicateHint h = new DuplicateHint();
            h.setCustomerId(id);
            CustomerBase base = customers.get(id);
            h.setFullName(base != null ? base.fullName() : null);
            h.setReason(reason);
            return h;
        }
    }

    private DuplicateIndex buildDuplicateIndex(Map<Integer, CustomerBase> loaded) {
        Map<String, Set<Integer>> byPhone = new HashMap<>();
        for (PhoneRow row : reader.allPhones()) {
            String key = ImportNormalizer.normalizePhone(row.phone());
            if (key == null) continue;
            byPhone.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(row.customerId());
        }
        byPhone.values().removeIf(ids -> ids.size() < 2);
        return new DuplicateIndex(byPhone, reader.sharedPlates());
    }

    // ================================================================ lọc / sắp xếp

    private Map<String, Integer> countTabs(List<CareCustomerRow> rows) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put(TAB_DUE, (int) rows.stream().filter(CareCustomerRow::isDue).count());
        counts.put(TAB_ALL, rows.size());
        for (CareStatus s : CareStatus.values()) counts.put(s.name(), 0);
        for (CareCustomerRow row : rows) counts.merge(row.getStatus(), 1, Integer::sum);
        return counts;
    }

    private Predicate<CareCustomerRow> tabFilter(String tab) {
        if (TAB_ALL.equals(tab)) return row -> true;
        if (TAB_DUE.equals(tab)) return CareCustomerRow::isDue;
        return row -> tab.equals(row.getStatus());
    }

    private Predicate<CareCustomerRow> searchFilter(String search) {
        String text = ImportNormalizer.stripDiacritics(search);
        if (text == null) return row -> true;
        String digits = search.replaceAll("[^0-9]", "");
        String plate = com.g42.platform.gms.vehicle.support.PlateKeys.normalize(search);
        return row -> {
            if (contains(ImportNormalizer.stripDiacritics(row.getFullName()), text)) return true;
            if (contains(ImportNormalizer.stripDiacritics(row.getCustomerCode()), text)) return true;
            if (contains(ImportNormalizer.stripDiacritics(row.getEmail()), text)) return true;
            if (digits.length() >= 3) {
                if (contains(digitsOf(row.getPhone()), digits)) return true;
                for (String p : row.getExtraPhones()) if (contains(digitsOf(p), digits)) return true;
            }
            if (plate != null && plate.length() >= 3) {
                for (VehicleBrief v : row.getVehicles()) {
                    if (contains(com.g42.platform.gms.vehicle.support.PlateKeys.normalize(v.getLicensePlate()), plate)) {
                        return true;
                    }
                }
            }
            for (VehicleBrief v : row.getVehicles()) {
                String vehicleName = ImportNormalizer.stripDiacritics(
                        (Objects.toString(v.getBrand(), "") + " " + Objects.toString(v.getModel(), "")));
                if (contains(vehicleName, text)) return true;
            }
            return false;
        };
    }

    private Predicate<CareCustomerRow> daysFilter(Integer minDays, Integer maxDays) {
        if ((minDays == null || minDays <= 0) && (maxDays == null || maxDays <= 0)) return row -> true;
        return row -> {
            Integer days = row.getDaysSinceLastVisit();
            if (days == null) return false;
            if (minDays != null && minDays > 0 && days < minDays) return false;
            return maxDays == null || maxDays <= 0 || days <= maxDays;
        };
    }

    private Predicate<CareCustomerRow> spendFilter(BigDecimal minSpend) {
        if (minSpend == null || minSpend.signum() <= 0) return row -> true;
        return row -> row.getTotalSpend().compareTo(minSpend) >= 0;
    }

    /** HAS_LEGACY: có lượt ở sổ cũ · HAS_SYSTEM: có phiếu trong phần mềm · LEGACY_ONLY: chỉ có sổ cũ. */
    private Predicate<CareCustomerRow> sourceFilter(String source) {
        if (source == null || source.isBlank()) return row -> true;
        return switch (source.trim().toUpperCase()) {
            case "HAS_LEGACY" -> row -> row.getLegacyVisitCount() > 0;
            case "HAS_SYSTEM" -> row -> row.getSystemVisitCount() > 0;
            case "LEGACY_ONLY" -> row -> row.getLegacyVisitCount() > 0 && row.getSystemVisitCount() == 0;
            default -> row -> true;
        };
    }

    private Predicate<CareCustomerRow> flagFilter(String flag) {
        if (flag == null || flag.isBlank()) return row -> true;
        return switch (flag.trim().toUpperCase()) {
            case "DUPLICATE" -> row -> !row.getDuplicates().isEmpty();
            case "INVALID_PHONE" -> row -> row.getPhone() != null && !row.isPhoneValid();
            case "RETURNED" -> CareCustomerRow::isReturnedAfterCall;
            case "HAS_CARE_NOTE" -> row -> row.getCareNote() != null;
            default -> row -> true;
        };
    }

    private Comparator<CareCustomerRow> comparator(String sort, String tab) {
        Comparator<CareCustomerRow> bySpendDesc = Comparator.comparing(CareCustomerRow::getTotalSpend).reversed();
        Comparator<CareCustomerRow> byLastVisit = Comparator.comparing(CareCustomerRow::getLastVisitAt,
                Comparator.nullsFirst(Comparator.naturalOrder()));
        Comparator<CareCustomerRow> byLastCall = Comparator.comparing(
                (CareCustomerRow r) -> r.getLastCall() != null ? r.getLastCall().getCalledAt() : null,
                Comparator.nullsFirst(Comparator.naturalOrder()));
        Comparator<CareCustomerRow> tie = Comparator.comparing(CareCustomerRow::getCustomerId);

        String key = sort == null || sort.isBlank() ? "PRIORITY" : sort.trim().toUpperCase();
        return switch (key) {
            case "SPEND_DESC" -> bySpendDesc.thenComparing(tie);
            case "LAST_VISIT_DESC" -> byLastVisit.reversed().thenComparing(tie);
            case "LAST_VISIT_ASC" -> Comparator.comparing(CareCustomerRow::getLastVisitAt,
                    Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(tie);
            case "LAST_CALL_DESC" -> byLastCall.reversed().thenComparing(tie);
            case "FOLLOW_UP_ASC" -> Comparator.comparing(CareCustomerRow::getFollowUpDate,
                    Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(bySpendDesc).thenComparing(tie);
            case "NAME_ASC" -> Comparator.comparing((CareCustomerRow r) -> Objects.toString(
                    ImportNormalizer.stripDiacritics(r.getFullName()), "￿")).thenComparing(tie);
            // Ưu tiên: hẹn gọi lại đã tới ngày (cũ nhất trước) → chưa gọi → chưa liên lạc được,
            // trong mỗi nhóm khách chi nhiều hơn lên trước.
            default -> Comparator.comparingInt(this::priorityGroup)
                    .thenComparing(CareCustomerRow::getFollowUpDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(bySpendDesc)
                    .thenComparing(tie);
        };
    }

    private int priorityGroup(CareCustomerRow row) {
        if (row.isDue() && row.getFollowUpDate() != null) return 0;
        if (CareStatus.NOT_CALLED.name().equals(row.getStatus())) return 1;
        if (row.isDue()) return 2;
        return 3;
    }

    // ================================================================ tiện ích

    /** Di động 10 số, hoặc cố định 11 số đầu 02. */
    private static boolean isCallable(String phone) {
        String normalized = ImportNormalizer.normalizePhone(phone);
        return ImportNormalizer.isValidPhone(normalized) || (normalized != null && normalized.matches("^02\\d{9}$"));
    }

    private static String digitsOf(String value) {
        return value == null ? null : value.replaceAll("[^0-9]", "");
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && needle != null && haystack.contains(needle);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void requireCustomer(int customerId) {
        if (!reader.customerExists(customerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng #" + customerId);
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
