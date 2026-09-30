package com.g42.platform.gms.customercare.infrastructure.jdbc;

import com.g42.platform.gms.customercare.api.dto.CareDtos.VehicleBrief;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Đọc gom nhóm mọi thứ màn gọi chăm sóc cần, mỗi nguồn MỘT truy vấn cho cả danh bạ.
 *
 * Dùng JdbcTemplate chứ không qua JPA: đây toàn là SUM/COUNT/MAX gộp theo khách trên năm
 * sáu bảng của các phân hệ khác nhau (phiếu, hoá đơn, sổ cũ, xe, SĐT phụ). Đi qua entity
 * thì hoặc N+1 truy vấn, hoặc phải kéo nguyên bảng lên bộ nhớ.
 *
 * Mọi hàm nhận onlyCustomerId: null = cả danh bạ (màn danh sách), có giá trị = một khách
 * (bảng chi tiết), để hai màn tính cùng một công thức.
 */
@Component
@RequiredArgsConstructor
public class CareDataReader {

    /** Hồ sơ dùng chung cho khách vãng lai (WalkInCustomerService) — không phải một người để gọi. */
    private static final String WALK_IN_CODE = "KHACH_LE";

    private final JdbcTemplate jdbc;

    public record CustomerBase(int customerId, String customerCode, String fullName, String phone,
                               String email, boolean doNotContact) {
    }

    public record VisitAgg(int count, LocalDateTime lastAt, BigDecimal spend) {
    }

    public record CallRow(int careCallId, int customerId, String phone, LocalDateTime calledAt, Integer staffId,
                          boolean reached, String outcome, String note, LocalDate followUpDate) {
    }

    public record LegacyCallRow(int legacyVisitId, int customerId, LocalDateTime visitedAt, boolean called,
                                boolean callSuccess, String note) {
    }

    public record CareProfileRow(String careNote, String preferredTime) {
    }

    public record PhoneRow(int customerId, String phone) {
    }

    public record TodayRow(int calls, int reached, int booked) {
    }

    // ---------------------------------------------------------------- khách

    /** Khách còn hiệu lực: bỏ hồ sơ khách lẻ dùng chung và tài khoản đã xoá. */
    public Map<Integer, CustomerBase> customers(Integer onlyCustomerId) {
        List<Object> args = new ArrayList<>();
        args.add(WALK_IN_CODE);
        String sql = "SELECT c.customer_id, c.customer_code, c.full_name, c.phone, c.email, c.do_not_contact"
                + " FROM customer_profile c"
                + " WHERE (c.customer_code IS NULL OR c.customer_code <> ?)"
                + " AND NOT EXISTS (SELECT 1 FROM customer_auth ca WHERE ca.customer_id = c.customer_id"
                + "                 AND ca.status = 'DELETED')"
                + only(onlyCustomerId, "c.customer_id", args);
        Map<Integer, CustomerBase> result = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            int id = rs.getInt(1);
            result.put(id, new CustomerBase(id, rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getBoolean(6)));
        }, args.toArray());
        return result;
    }

    /** Tên của một nhóm khách bất kỳ — cho gợi ý trùng hồ sơ ở màn chi tiết. */
    public Map<Integer, String> namesOf(Collection<Integer> customerIds) {
        Map<Integer, String> result = new HashMap<>();
        if (customerIds == null || customerIds.isEmpty()) return result;
        String in = String.join(",", Collections.nCopies(customerIds.size(), "?"));
        jdbc.query("SELECT customer_id, full_name FROM customer_profile WHERE customer_id IN (" + in + ")",
                rs -> {
                    result.put(rs.getInt(1), rs.getString(2));
                }, customerIds.toArray());
        return result;
    }

    /**
     * SĐT chính + SĐT phụ của MỌI khách — cần cả danh bạ để dò số trùng giữa các hồ sơ.
     *
     * Hai truy vấn riêng chứ không UNION: cột phone của hai bảng khác collation
     * (utf8mb4_unicode_ci vs utf8mb4_0900_ai_ci), UNION báo "Illegal mix of collations".
     */
    public List<PhoneRow> allPhones() {
        List<PhoneRow> rows = new ArrayList<>();
        for (String table : List.of("customer_profile", "customer_phone")) {
            jdbc.query("SELECT customer_id, phone FROM " + table + " WHERE phone IS NOT NULL AND phone <> ''",
                    rs -> {
                        rows.add(new PhoneRow(rs.getInt(1), rs.getString(2)));
                    });
        }
        return rows;
    }

    /** SĐT phụ theo khách (customer_phone), theo thứ tự thêm vào. */
    public Map<Integer, List<String>> extraPhones(Integer onlyCustomerId) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT customer_id, phone FROM customer_phone p WHERE p.phone IS NOT NULL AND p.phone <> ''"
                + only(onlyCustomerId, "p.customer_id", args)
                + " ORDER BY p.customer_id, p.customer_phone_id";
        Map<Integer, List<String>> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            result.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>()).add(rs.getString(2));
        }, args.toArray());
        return result;
    }

    public Map<Integer, List<VehicleBrief>> vehicles(Integer onlyCustomerId) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT v.vehicle_id, v.customer_id, v.license_plate, v.brand, v.model FROM vehicle v"
                + " WHERE v.customer_id IS NOT NULL"
                + only(onlyCustomerId, "v.customer_id", args)
                + " ORDER BY v.customer_id, v.vehicle_id";
        Map<Integer, List<VehicleBrief>> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            VehicleBrief v = new VehicleBrief();
            v.setVehicleId(rs.getInt(1));
            v.setLicensePlate(rs.getString(3));
            v.setBrand(rs.getString(4));
            v.setModel(rs.getString(5));
            result.computeIfAbsent(rs.getInt(2), k -> new ArrayList<>()).add(v);
        }, args.toArray());
        return result;
    }

    /**
     * Biển số (đã chuẩn hoá) nằm trên từ hai hồ sơ khác nhau trở lên. Biển số được phép
     * trùng (xe dùng chung — changeset 039) nên đây chỉ là gợi ý, không phải lỗi.
     */
    public Map<String, Set<Integer>> sharedPlates() {
        Map<String, Set<Integer>> result = new HashMap<>();
        jdbc.query("SELECT v.plate_key, v.customer_id FROM vehicle v"
                        + " JOIN (SELECT plate_key FROM vehicle"
                        + "       WHERE plate_key IS NOT NULL AND plate_key <> '' AND customer_id IS NOT NULL"
                        + "       GROUP BY plate_key HAVING COUNT(DISTINCT customer_id) > 1) d ON d.plate_key = v.plate_key"
                        + " WHERE v.customer_id IS NOT NULL",
                rs -> {
                    result.computeIfAbsent(rs.getString(1), k -> new LinkedHashSet<>()).add(rs.getInt(2));
                });
        return result;
    }

    // ---------------------------------------------------------------- lượt đến + tiền

    /**
     * Phiếu dịch vụ trong phần mềm — cùng điều kiện với CustomerVisitStatsService
     * (LegacyVisitRepository.aggregateTicketVisits) để "Lần cuối đến" khớp mọi màn khác.
     */
    public Map<Integer, VisitAgg> systemVisits(Integer onlyCustomerId) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT st.customer_id, COUNT(*), MAX(st.received_at) FROM service_ticket st"
                + " WHERE (st.is_deleted = 0 OR st.is_deleted IS NULL)"
                + " AND st.ticket_status IN ('COMPLETED','PAID','CANCELLED')"
                + " AND st.received_at IS NOT NULL"
                + only(onlyCustomerId, "st.customer_id", args)
                + " GROUP BY st.customer_id";
        Map<Integer, VisitAgg> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            result.put(rs.getInt(1), new VisitAgg(rs.getInt(2), toDateTime(rs.getTimestamp(3)), BigDecimal.ZERO));
        }, args.toArray());
        return result;
    }

    /**
     * Tiền đã thu theo hoá đơn: mỗi phiếu lấy hoá đơn PAID mới nhất (một phiếu có thể có
     * nhiều hoá đơn khi lập lại), bỏ phiếu đã xoá.
     */
    public Map<Integer, BigDecimal> systemSpend(Integer onlyCustomerId) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT st.customer_id, SUM(b.final_amount) FROM service_bill b"
                + " JOIN (SELECT service_ticket_id, MAX(bill_id) AS bill_id FROM service_bill"
                + "       WHERE payment_status = 'PAID' GROUP BY service_ticket_id) lb ON lb.bill_id = b.bill_id"
                + " JOIN service_ticket st ON st.service_ticket_id = b.service_ticket_id"
                + " WHERE (st.is_deleted = 0 OR st.is_deleted IS NULL)"
                + only(onlyCustomerId, "st.customer_id", args)
                + " GROUP BY st.customer_id";
        Map<Integer, BigDecimal> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            BigDecimal amount = rs.getBigDecimal(2);
            result.put(rs.getInt(1), amount != null ? amount : BigDecimal.ZERO);
        }, args.toArray());
        return result;
    }

    /** Lượt sổ cũ: số lượt, lần cuối, tổng tiền chép tay. */
    public Map<Integer, VisitAgg> legacyVisits(Integer onlyCustomerId) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT lv.customer_id, COUNT(*), MAX(lv.visited_at), SUM(lv.total_amount) FROM legacy_visit lv"
                + " WHERE 1 = 1"
                + only(onlyCustomerId, "lv.customer_id", args)
                + " GROUP BY lv.customer_id";
        Map<Integer, VisitAgg> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            BigDecimal amount = rs.getBigDecimal(4);
            result.put(rs.getInt(1), new VisitAgg(rs.getInt(2), toDateTime(rs.getTimestamp(3)),
                    amount != null ? amount : BigDecimal.ZERO));
        }, args.toArray());
        return result;
    }

    // ---------------------------------------------------------------- cuộc gọi

    /** Số cuộc gọi trong phần mềm theo khách. */
    public Map<Integer, Integer> callCounts(Integer onlyCustomerId) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT cc.customer_id, COUNT(*) FROM customer_care_call cc WHERE 1 = 1"
                + only(onlyCustomerId, "cc.customer_id", args)
                + " GROUP BY cc.customer_id";
        Map<Integer, Integer> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            result.put(rs.getInt(1), rs.getInt(2));
        }, args.toArray());
        return result;
    }

    /**
     * Cuộc gọi gần nhất của mỗi khách. Không dùng hàm cửa sổ để chạy được cả trên MySQL cũ:
     * nối với MAX(called_at), trùng giờ thì giữ dòng có id lớn hơn.
     */
    public Map<Integer, CallRow> latestCalls(Integer onlyCustomerId) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT cc.care_call_id, cc.customer_id, cc.phone, cc.called_at, cc.staff_id, cc.reached,"
                + "       cc.outcome, cc.note, cc.follow_up_date"
                + " FROM customer_care_call cc"
                + " JOIN (SELECT customer_id, MAX(called_at) AS mx FROM customer_care_call WHERE 1 = 1"
                + only(onlyCustomerId, "customer_id", args)
                + "       GROUP BY customer_id) m ON m.customer_id = cc.customer_id AND m.mx = cc.called_at";
        Map<Integer, CallRow> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            CallRow row = callRow(rs);
            result.merge(row.customerId(), row, (a, b) -> a.careCallId() >= b.careCallId() ? a : b);
        }, args.toArray());
        return result;
    }

    public List<CallRow> callsOf(int customerId) {
        return jdbc.query("SELECT care_call_id, customer_id, phone, called_at, staff_id, reached, outcome, note,"
                        + " follow_up_date FROM customer_care_call WHERE customer_id = ?"
                        + " ORDER BY called_at DESC, care_call_id DESC",
                (rs, i) -> callRow(rs), customerId);
    }

    /** Kết quả gọi chép ở sổ cũ: lượt có tick "Đã gọi" hoặc có ghi chú gọi. */
    public List<LegacyCallRow> legacyCalls(Integer onlyCustomerId) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT lv.legacy_visit_id, lv.customer_id, lv.visited_at, lv.called, lv.call_success, lv.call_note"
                + " FROM legacy_visit lv"
                + " WHERE (lv.called = 1 OR (lv.call_note IS NOT NULL AND lv.call_note <> ''))"
                + only(onlyCustomerId, "lv.customer_id", args)
                + " ORDER BY lv.visited_at DESC, lv.legacy_visit_id DESC";
        return jdbc.query(sql, (rs, i) -> new LegacyCallRow(rs.getInt(1), rs.getInt(2),
                toDateTime(rs.getTimestamp(3)), rs.getBoolean(4), rs.getBoolean(5), rs.getString(6)), args.toArray());
    }

    public Map<Integer, CareProfileRow> careProfiles(Integer onlyCustomerId) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT customer_id, care_note, preferred_time FROM customer_care_profile WHERE 1 = 1"
                + only(onlyCustomerId, "customer_id", args);
        Map<Integer, CareProfileRow> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            result.put(rs.getInt(1), new CareProfileRow(rs.getString(2), rs.getString(3)));
        }, args.toArray());
        return result;
    }

    public Map<Integer, String> staffNames() {
        Map<Integer, String> result = new HashMap<>();
        jdbc.query("SELECT staff_id, full_name FROM staff_profile", rs -> {
            result.put(rs.getInt(1), rs.getString(2));
        });
        return result;
    }

    /** Số cuộc gọi trong ngày — ô thống kê đầu trang. */
    public TodayRow today(LocalDate day) {
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        return jdbc.queryForObject("SELECT COUNT(*),"
                        + " COALESCE(SUM(CASE WHEN reached = 1 THEN 1 ELSE 0 END), 0),"
                        + " COALESCE(SUM(CASE WHEN outcome = 'BOOKED' THEN 1 ELSE 0 END), 0)"
                        + " FROM customer_care_call WHERE called_at >= ? AND called_at < ?",
                (rs, i) -> new TodayRow(rs.getInt(1), rs.getInt(2), rs.getInt(3)),
                Timestamp.valueOf(start), Timestamp.valueOf(end));
    }

    public boolean customerExists(int customerId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM customer_profile WHERE customer_id = ?",
                Integer.class, customerId);
        return count != null && count > 0;
    }

    /** Cờ do_not_contact nằm trên customer_profile (changeset 022-6), entity thuộc phân hệ khác. */
    public void setDoNotContact(int customerId, boolean value) {
        jdbc.update("UPDATE customer_profile SET do_not_contact = ? WHERE customer_id = ?", value, customerId);
    }

    // ---------------------------------------------------------------- tiện ích

    private static String only(Integer customerId, String column, List<Object> args) {
        if (customerId == null) return "";
        args.add(customerId);
        return " AND " + column + " = ?";
    }

    private static CallRow callRow(ResultSet rs) throws SQLException {
        int staff = rs.getInt(5);
        Integer staffId = rs.wasNull() ? null : staff;
        Date follow = rs.getDate(9);
        return new CallRow(rs.getInt(1), rs.getInt(2), rs.getString(3), toDateTime(rs.getTimestamp(4)),
                staffId, rs.getBoolean(6), rs.getString(7), rs.getString(8),
                follow != null ? follow.toLocalDate() : null);
    }

    private static LocalDateTime toDateTime(Timestamp value) {
        return value != null ? value.toLocalDateTime() : null;
    }
}
