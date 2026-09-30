package com.g42.platform.gms.dashboard.application.service;

import com.g42.platform.gms.dashboard.api.dto.DashboardSummaryResponse;
import com.g42.platform.gms.dashboard.api.dto.DashboardSummaryResponse.Bookings;
import com.g42.platform.gms.dashboard.api.dto.DashboardSummaryResponse.Customers;
import com.g42.platform.gms.dashboard.api.dto.DashboardSummaryResponse.Personal;
import com.g42.platform.gms.dashboard.api.dto.DashboardSummaryResponse.Revenue;
import com.g42.platform.gms.dashboard.api.dto.DashboardSummaryResponse.Tickets;
import com.g42.platform.gms.dashboard.api.dto.DashboardSummaryResponse.TrendPoint;
import com.g42.platform.gms.dashboard.infrastructure.DashboardDailySummaryStore;
import com.g42.platform.gms.dashboard.infrastructure.DashboardDailySummaryStore.DailyRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Số liệu cho trang /dashboard, đọc qua hai tầng cache để mở trang không phải quét lại dữ liệu gốc:
 *
 * <ol>
 *   <li><b>Bảng {@code dashboard_daily_summary}</b> — mỗi ngày một dòng, tính bằng
 *       {@link DashboardDailyAggregator}. Ngày đã qua hẳn (computed_at sau 00:00 của ngày+2) coi
 *       như đã chốt và không tính lại khi đọc; hôm nay / hôm qua được tính lại khi dòng cũ hơn
 *       {@link #OPEN_DAY_TTL}; ngày chưa có dòng thì tính bù. Job đêm tính lại
 *       {@link #NIGHTLY_DAYS} ngày gần nhất để bắt thay đổi muộn.</li>
 *   <li><b>Cache bộ nhớ {@link #RESPONSE_TTL}</b> cho cả phản hồi — nhiều người cùng mở dashboard
 *       hoặc bấm qua lại giữa các kỳ không chạm DB.</li>
 * </ol>
 *
 * Số "hiện tại" không gắn với ngày nào (yêu cầu đặt lịch đang chờ, tổng số khách) và số của cá
 * nhân người xem được truy vấn trực tiếp — đều là COUNT nhỏ, và cũng nằm trong cache bộ nhớ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardSummaryService {

    static final Duration OPEN_DAY_TTL = Duration.ofMinutes(2);
    static final Duration RESPONSE_TTL = Duration.ofSeconds(60);
    static final Duration EARLIEST_DATE_TTL = Duration.ofHours(1);
    static final int NIGHTLY_DAYS = 62;
    private static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);
    private static final int MAX_CACHE_ENTRIES = 300;
    private static final String WALK_IN_CUSTOMER_CODE = "KHACH_LE";
    private static final BigDecimal VAT_DIVISOR = new BigDecimal("1.1");

    public enum Granularity { DAY, MONTH, YEAR }

    /** Nhóm số liệu người xem được thấy. */
    public record Access(boolean revenue, boolean bookings, boolean customers) {
    }

    private final DashboardDailySummaryStore store;
    private final DashboardDailyAggregator aggregator;
    private final JdbcTemplate jdbc;

    private final Map<String, CachedResponse> responseCache = new ConcurrentHashMap<>();
    private final Object fillLock = new Object();
    private volatile LocalDate earliestDate;
    private volatile LocalDateTime earliestDateExpiresAt = LocalDateTime.MIN;

    private record CachedResponse(DashboardSummaryResponse value, LocalDateTime expiresAt) {
    }

    /**
     * @param from null = kỳ "Tất cả" (từ ngày sớm nhất có dữ liệu)
     * @param to   null = hôm nay; ngày tương lai bị cắt về hôm nay
     */
    public DashboardSummaryResponse getSummary(LocalDate from, LocalDate to, Integer staffId, Access access) {
        LocalDate today = LocalDate.now();
        boolean allTime = from == null;
        LocalDate end = to == null || to.isAfter(today) ? today : to;
        LocalDate start = allTime ? earliestDataDate() : from;
        if (start.isAfter(end)) {
            start = end;
        }

        String key = String.join("|", String.valueOf(start), String.valueOf(end), String.valueOf(allTime),
                String.valueOf(staffId), String.valueOf(access.revenue()), String.valueOf(access.bookings()),
                String.valueOf(access.customers()));
        LocalDateTime now = LocalDateTime.now();
        CachedResponse cached = responseCache.get(key);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.value();
        }

        DashboardSummaryResponse response = build(start, end, allTime, staffId, access);
        if (responseCache.size() >= MAX_CACHE_ENTRIES) {
            responseCache.entrySet().removeIf(e -> !e.getValue().expiresAt().isAfter(now));
        }
        responseCache.put(key, new CachedResponse(response, now.plus(RESPONSE_TTL)));
        return response;
    }

    /** Tính lại cả kỳ (bỏ qua trạng thái đã chốt) — dùng sau khi sửa dữ liệu cũ. Trả về số ngày đã tính. */
    public int refresh(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        LocalDate end = to == null || to.isAfter(today) ? today : to;
        LocalDate start = from == null ? earliestDataDate() : from;
        if (start.isAfter(end)) {
            start = end;
        }
        int days;
        synchronized (fillLock) {
            Map<LocalDate, DailyRow> rows = aggregator.compute(start, end);
            store.upsertAll(rows.values());
            days = rows.size();
        }
        responseCache.clear();
        earliestDateExpiresAt = LocalDateTime.MIN;
        return days;
    }

    /** Bắt các thay đổi muộn: nhập bù phiếu ngày cũ, lịch hẹn đánh "không đến" sau vài hôm, sửa hồ sơ khách... */
    @Scheduled(cron = "0 40 2 * * *")
    public void nightlyRefresh() {
        LocalDate today = LocalDate.now();
        try {
            int days = refresh(today.minusDays(NIGHTLY_DAYS), today.minusDays(1));
            log.info("Dashboard summary: đã tính lại {} ngày gần nhất", days);
        } catch (RuntimeException ex) {
            log.error("Dashboard summary: tính lại theo lịch đêm thất bại", ex);
        }
    }

    // ------------------------------------------------------------------------------------------

    private DashboardSummaryResponse build(LocalDate start, LocalDate end, boolean allTime,
                                           Integer staffId, Access access) {
        // Kỳ ngắn vẫn vẽ đường xu hướng 7 ngày.
        LocalDate trendStart = start.isAfter(end.minusDays(6)) ? end.minusDays(6) : start;
        List<DailyRow> days = loadDays(trendStart, end);
        List<DailyRow> inRange = days.stream().filter(r -> !r.getDate().isBefore(start)).toList();

        PersonalDays personalDays = staffId != null ? loadPersonal(staffId, trendStart, end) : null;
        Granularity granularity = granularityOf(trendStart, end);

        return DashboardSummaryResponse.builder()
                .from(start)
                .to(end)
                .trendFrom(trendStart)
                .trendGranularity(granularity.name())
                .allTime(allTime)
                .generatedAt(LocalDateTime.now())
                .revenue(access.revenue() ? revenue(inRange) : null)
                .bookings(access.bookings() ? bookings(inRange, start, end, allTime) : null)
                .customers(access.customers() ? customers(inRange, allTime) : null)
                .tickets(Tickets.builder().completed(sumInt(inRange, DailyRow::getTicketsCompleted)).build())
                .personal(personalDays != null ? personalDays.summarize(start) : null)
                .trend(trend(days, personalDays, access, granularity))
                .build();
    }

    /** Đọc dòng cache của khoảng ngày, tính bù / tính lại những ngày thiếu hoặc chưa chốt. */
    private List<DailyRow> loadDays(LocalDate from, LocalDate to) {
        Map<LocalDate, DailyRow> byDate = index(store.findBetween(from, to));
        if (!staleDates(byDate, from, to).isEmpty()) {
            synchronized (fillLock) {
                // Luồng khác có thể vừa tính xong trong lúc chờ khoá — đọc lại rồi mới tính.
                byDate = index(store.findBetween(from, to));
                for (LocalDate[] run : runs(staleDates(byDate, from, to))) {
                    Map<LocalDate, DailyRow> computed = aggregator.compute(run[0], run[1]);
                    store.upsertAll(computed.values());
                    byDate.putAll(computed);
                }
            }
        }
        return new ArrayList<>(byDate.values());
    }

    private static Map<LocalDate, DailyRow> index(List<DailyRow> rows) {
        Map<LocalDate, DailyRow> map = new TreeMap<>();
        rows.forEach(r -> map.put(r.getDate(), r));
        return map;
    }

    private static List<LocalDate> staleDates(Map<LocalDate, DailyRow> byDate, LocalDate from, LocalDate to) {
        LocalDateTime now = LocalDateTime.now();
        List<LocalDate> stale = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            DailyRow row = byDate.get(d);
            if (row == null || needsRefresh(row, now)) {
                stale.add(d);
            }
        }
        return stale;
    }

    /** Dòng tính lúc ngày đó chưa qua hẳn (+1 ngày cho việc cập nhật muộn) thì chưa chốt. */
    private static boolean needsRefresh(DailyRow row, LocalDateTime now) {
        LocalDateTime sealedAfter = row.getDate().plusDays(2).atStartOfDay();
        return row.getComputedAt().isBefore(sealedAfter)
                && row.getComputedAt().isBefore(now.minus(OPEN_DAY_TTL));
    }

    /** Gom các ngày liên tiếp thành đoạn để mỗi đoạn chỉ cần một lượt truy vấn. */
    private static List<LocalDate[]> runs(List<LocalDate> dates) {
        List<LocalDate[]> runs = new ArrayList<>();
        LocalDate runStart = null;
        LocalDate prev = null;
        for (LocalDate d : dates) {
            if (runStart == null) {
                runStart = d;
            } else if (!d.equals(prev.plusDays(1))) {
                runs.add(new LocalDate[]{runStart, prev});
                runStart = d;
            }
            prev = d;
        }
        if (runStart != null) {
            runs.add(new LocalDate[]{runStart, prev});
        }
        return runs;
    }

    // ---- Các nhóm số liệu --------------------------------------------------------------------

    private static Revenue revenue(List<DailyRow> rows) {
        BigDecimal paid = sumMoney(rows, DailyRow::getPaidRevenue);
        return Revenue.builder()
                .paidRevenue(paid)
                .revenueNoTax(paid.divide(VAT_DIVISOR, 0, RoundingMode.HALF_UP))
                .discountAmount(sumMoney(rows, DailyRow::getDiscountAmount))
                .paidBillCount(sumInt(rows, DailyRow::getPaidBillCount))
                .legacyRevenue(sumMoney(rows, DailyRow::getLegacyRevenue))
                .legacyVisitCount(sumInt(rows, DailyRow::getLegacyVisitCount))
                .build();
    }

    private Bookings bookings(List<DailyRow> rows, LocalDate start, LocalDate end, boolean allTime) {
        long done = sumInt(rows, DailyRow::getBookingsDone);
        long notArrived = sumInt(rows, DailyRow::getBookingsNotArrived);
        long cancelled = sumInt(rows, DailyRow::getBookingsCancelled);
        long concluded = done + notArrived + cancelled;

        Long pendingDue = jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking_request WHERE status = 'PENDING' AND scheduled_date <= ?",
                Long.class, Date.valueOf(end));
        Long pendingInRange = allTime
                ? jdbc.queryForObject("SELECT COUNT(*) FROM booking_request WHERE status = 'PENDING'", Long.class)
                : jdbc.queryForObject("SELECT COUNT(*) FROM booking_request WHERE status = 'PENDING'"
                                + " AND created_at >= ? AND created_at < ?", Long.class,
                        Timestamp.valueOf(start.atStartOfDay()), Timestamp.valueOf(end.plusDays(1).atStartOfDay()));

        return Bookings.builder()
                .pendingDue(pendingDue != null ? pendingDue : 0)
                .pendingInRange(pendingInRange != null ? pendingInRange : 0)
                .requests(sumInt(rows, DailyRow::getBookingRequests))
                .scheduled(sumInt(rows, DailyRow::getBookingsScheduled))
                .done(done)
                .notArrived(notArrived)
                .cancelled(cancelled)
                .successRate(concluded > 0 ? Math.round(done * 1000.0 / concluded) / 10.0 : null)
                .build();
    }

    /**
     * Kỳ "Tất cả" đếm thẳng mọi hồ sơ (cả hồ sơ cũ không có created_at); kỳ khác cộng từ cache
     * theo ngày tạo hồ sơ.
     */
    private Customers customers(List<DailyRow> rows, boolean allTime) {
        Map<String, Long> byGender = new HashMap<>();
        jdbc.query("SELECT gender, COUNT(*) FROM customer_profile"
                        + " WHERE customer_code IS NULL OR customer_code <> ? GROUP BY gender",
                rs -> {
                    byGender.put(String.valueOf(rs.getString(1)), rs.getLong(2));
                }, WALK_IN_CUSTOMER_CODE);
        long total = byGender.values().stream().mapToLong(Long::longValue).sum();

        if (allTime) {
            long male = byGender.getOrDefault("MALE", 0L);
            long female = byGender.getOrDefault("FEMALE", 0L);
            long other = byGender.getOrDefault("OTHER", 0L);
            return Customers.builder()
                    .newCustomers(total).male(male).female(female).other(other)
                    .unknown(total - male - female - other)
                    .totalCustomers(total)
                    .build();
        }
        long created = sumInt(rows, DailyRow::getNewCustomers);
        long male = sumInt(rows, DailyRow::getNewCustomersMale);
        long female = sumInt(rows, DailyRow::getNewCustomersFemale);
        long other = sumInt(rows, DailyRow::getNewCustomersOther);
        return Customers.builder()
                .newCustomers(created).male(male).female(female).other(other)
                .unknown(created - male - female - other)
                .totalCustomers(total)
                .build();
    }

    // ---- Số liệu cá nhân ---------------------------------------------------------------------

    /** Chấm công + phiếu đã làm của một nhân viên, theo ngày. */
    private static final class PersonalDays {
        final Map<LocalDate, Double> hours = new TreeMap<>();
        final Set<LocalDate> presentDates = new HashSet<>();
        final Set<LocalDate> scheduledDates = new HashSet<>();
        final Map<LocalDate, Integer> tickets = new TreeMap<>();
        final List<Object[]> statusByDate = new ArrayList<>();

        Personal summarize(LocalDate start) {
            long onTime = 0;
            long late = 0;
            long early = 0;
            long absent = 0;
            for (Object[] s : statusByDate) {
                if (((LocalDate) s[0]).isBefore(start)) {
                    continue;
                }
                switch ((String) s[1]) {
                    case "PRESENT" -> onTime++;
                    case "LATE" -> late++;
                    case "EARLY_LEAVE" -> early++;
                    case "ABSENT" -> absent++;
                    default -> {
                    }
                }
            }
            double workHours = hours.entrySet().stream().filter(e -> !e.getKey().isBefore(start))
                    .mapToDouble(Map.Entry::getValue).sum();
            return Personal.builder()
                    .workHours(Math.round(workHours * 10) / 10.0)
                    .presentDays(presentDates.stream().filter(d -> !d.isBefore(start)).count())
                    .scheduledDays(scheduledDates.stream().filter(d -> !d.isBefore(start)).count())
                    .onTimeDays(onTime)
                    .lateDays(late)
                    .earlyLeaveDays(early)
                    .absentDays(absent)
                    .completedTickets(tickets.entrySet().stream().filter(e -> !e.getKey().isBefore(start))
                            .mapToLong(Map.Entry::getValue).sum())
                    .build();
        }
    }

    private PersonalDays loadPersonal(Integer staffId, LocalDate from, LocalDate to) {
        PersonalDays p = new PersonalDays();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        jdbc.query("""
                        SELECT ac.attendance_date, ac.status, ac.check_in_time, ac.check_out_time,
                               ws.start_time, ws.end_time
                        FROM attendance_checkin ac
                        LEFT JOIN work_shift ws ON ws.shift_id = ac.shift_id
                        WHERE ac.staff_id = ? AND ac.attendance_date BETWEEN ? AND ?
                        """,
                rs -> {
                    LocalDate date = rs.getDate(1).toLocalDate();
                    String status = String.valueOf(rs.getString(2)).toUpperCase();
                    LocalTime checkIn = toLocalTime(rs.getTime(3));
                    LocalTime checkOut = toLocalTime(rs.getTime(4));
                    LocalTime shiftStart = toLocalTime(rs.getTime(5));
                    LocalTime shiftEnd = toLocalTime(rs.getTime(6));

                    boolean attended = "PRESENT".equals(status) || "LATE".equals(status) || "EARLY_LEAVE".equals(status);
                    if (!date.isAfter(today) && !"OFF".equals(status)) {
                        p.scheduledDates.add(date);
                    }
                    if (attended) {
                        p.presentDates.add(date);
                    }
                    p.statusByDate.add(new Object[]{date, status});

                    double h = workedHours(date, checkIn, checkOut, shiftStart, shiftEnd, attended, today, now);
                    if (h > 0) {
                        p.hours.merge(date, h, Double::sum);
                    }
                }, staffId, Date.valueOf(from), Date.valueOf(to));

        jdbc.query("""
                        SELECT DATE(t.completed_at), COUNT(DISTINCT t.service_ticket_id)
                        FROM service_ticket_assignment a
                        JOIN service_ticket t ON t.service_ticket_id = a.service_ticket_id
                        WHERE a.staff_id = ?
                          AND t.completed_at >= ? AND t.completed_at < ?
                          AND t.ticket_status IN ('COMPLETED', 'PAID')
                          AND COALESCE(t.is_deleted, 0) = 0
                        GROUP BY DATE(t.completed_at)
                        """,
                rs -> {
                    p.tickets.put(rs.getDate(1).toLocalDate(), rs.getInt(2));
                }, staffId, Timestamp.valueOf(from.atStartOfDay()), Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
        return p;
    }

    /**
     * Giờ vào → giờ ra thật. Chưa chấm ra: hôm nay tính tới hiện tại, ngày cũ lấy độ dài ca —
     * không bao giờ vượt độ dài ca khi đang đếm "tới hiện tại".
     */
    private static double workedHours(LocalDate date, LocalTime checkIn, LocalTime checkOut,
                                      LocalTime shiftStart, LocalTime shiftEnd, boolean attended,
                                      LocalDate today, LocalDateTime now) {
        long shiftMinutes = shiftStart != null && shiftEnd != null
                ? Math.max(0, ChronoUnit.MINUTES.between(shiftStart, shiftEnd)) : 0;
        if (checkIn != null && checkOut != null) {
            return Math.max(0, ChronoUnit.MINUTES.between(checkIn, checkOut)) / 60.0;
        }
        if (!attended) {
            return 0;
        }
        if (checkIn != null && date.equals(today)) {
            long elapsed = Math.max(0, ChronoUnit.MINUTES.between(checkIn, now.toLocalTime()));
            return (shiftMinutes > 0 ? Math.min(elapsed, shiftMinutes) : elapsed) / 60.0;
        }
        return shiftMinutes / 60.0;
    }

    private static LocalTime toLocalTime(Time time) {
        return time != null ? time.toLocalTime() : null;
    }

    // ---- Xu hướng ----------------------------------------------------------------------------

    /** Ngắn thì theo ngày, tới ~3 năm theo tháng, dài hơn theo năm — giữ số điểm trên biểu đồ vừa phải. */
    static Granularity granularityOf(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days <= 62) {
            return Granularity.DAY;
        }
        return days <= 1100 ? Granularity.MONTH : Granularity.YEAR;
    }

    private static LocalDate bucketOf(LocalDate date, Granularity g) {
        return switch (g) {
            case DAY -> date;
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
        };
    }

    private static List<TrendPoint> trend(List<DailyRow> days, PersonalDays personal, Access access, Granularity g) {
        Map<LocalDate, TrendPoint> buckets = new TreeMap<>();
        for (DailyRow r : days) {
            TrendPoint p = buckets.computeIfAbsent(bucketOf(r.getDate(), g), d -> emptyPoint(d, access, personal != null));
            if (access.revenue()) {
                p.setPaidRevenue(p.getPaidRevenue().add(r.getPaidRevenue()));
                p.setLegacyRevenue(p.getLegacyRevenue().add(r.getLegacyRevenue()));
                p.setPaidBillCount(p.getPaidBillCount() + r.getPaidBillCount());
            }
            if (access.customers()) {
                p.setNewCustomers(p.getNewCustomers() + r.getNewCustomers());
            }
            if (access.bookings()) {
                p.setBookingRequests(p.getBookingRequests() + r.getBookingRequests());
                p.setBookingsScheduled(p.getBookingsScheduled() + r.getBookingsScheduled());
                p.setBookingsDone(p.getBookingsDone() + r.getBookingsDone());
                p.setBookingsNotArrived(p.getBookingsNotArrived() + r.getBookingsNotArrived());
                p.setBookingsCancelled(p.getBookingsCancelled() + r.getBookingsCancelled());
            }
            p.setTicketsCompleted(p.getTicketsCompleted() + r.getTicketsCompleted());
            if (personal != null) {
                p.setWorkHours(p.getWorkHours() + personal.hours.getOrDefault(r.getDate(), 0.0));
                p.setPresentDays(p.getPresentDays() + (personal.presentDates.contains(r.getDate()) ? 1 : 0));
                p.setPersonalTicketsCompleted(p.getPersonalTicketsCompleted() + personal.tickets.getOrDefault(r.getDate(), 0));
            }
        }
        buckets.values().forEach(p -> {
            if (p.getWorkHours() != null) {
                p.setWorkHours(Math.round(p.getWorkHours() * 10) / 10.0);
            }
        });
        return new ArrayList<>(buckets.values());
    }

    private static TrendPoint emptyPoint(LocalDate date, Access access, boolean withPersonal) {
        TrendPoint p = TrendPoint.builder().date(date).ticketsCompleted(0).build();
        if (access.revenue()) {
            p.setPaidRevenue(BigDecimal.ZERO);
            p.setLegacyRevenue(BigDecimal.ZERO);
            p.setPaidBillCount(0);
        }
        if (access.customers()) {
            p.setNewCustomers(0);
        }
        if (access.bookings()) {
            p.setBookingRequests(0);
            p.setBookingsScheduled(0);
            p.setBookingsDone(0);
            p.setBookingsNotArrived(0);
            p.setBookingsCancelled(0);
        }
        if (withPersonal) {
            p.setWorkHours(0.0);
            p.setPresentDays(0);
            p.setPersonalTicketsCompleted(0);
        }
        return p;
    }

    // ---- Ngày sớm nhất có dữ liệu (kỳ "Tất cả") ------------------------------------------------

    private LocalDate earliestDataDate() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate cached = earliestDate;
        if (cached != null && earliestDateExpiresAt.isAfter(now)) {
            return cached;
        }
        LocalDate earliest = LocalDate.now();
        for (String sql : List.of(
                "SELECT MIN(DATE(created_at)) FROM customer_profile",
                "SELECT MIN(DATE(visited_at)) FROM legacy_visit",
                "SELECT MIN(DATE(received_at)) FROM service_ticket",
                "SELECT MIN(scheduled_date) FROM booking",
                "SELECT MIN(DATE(created_at)) FROM booking_request")) {
            Date min = jdbc.queryForObject(sql, Date.class);
            if (min != null) {
                LocalDate d = min.toLocalDate();
                if (d.isBefore(earliest) && !d.isBefore(MIN_DATE)) {
                    earliest = d;
                }
            }
        }
        earliestDate = earliest;
        earliestDateExpiresAt = now.plus(EARLIEST_DATE_TTL);
        return earliest;
    }

    // ---- Tiện ích ----------------------------------------------------------------------------

    private static BigDecimal sumMoney(List<DailyRow> rows, Function<DailyRow, BigDecimal> getter) {
        return rows.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static long sumInt(List<DailyRow> rows, Function<DailyRow, Integer> getter) {
        return rows.stream().mapToLong(getter::apply).sum();
    }
}
