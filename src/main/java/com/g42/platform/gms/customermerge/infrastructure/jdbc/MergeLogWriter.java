package com.g42.platform.gms.customermerge.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Ghi customer_merge_log và diễn giải kết quả chuyển tham chiếu thành câu dễ hiểu. */
public final class MergeLogWriter {

    public static final String TYPE_CUSTOMER = "CUSTOMER";
    public static final String TYPE_VEHICLE = "VEHICLE";

    /** Nhãn tiếng Việt cho các bảng hay gặp; bảng lạ thì hiện nguyên tên bảng. */
    private static final Map<String, String> TABLE_LABELS = Map.ofEntries(
            Map.entry("service_ticket", "phiếu dịch vụ"),
            Map.entry("booking", "lịch hẹn"),
            Map.entry("booking_request", "yêu cầu đặt lịch"),
            Map.entry("odometer_history", "lần ghi số km"),
            Map.entry("legacy_visit", "lượt sổ cũ"),
            Map.entry("service_reminder", "nhắc lịch bảo dưỡng"),
            Map.entry("part_warranty", "bảo hành phụ tùng"),
            Map.entry("vehicle_specification", "thông số xe"),
            Map.entry("vehicle", "xe"),
            Map.entry("customer_points_history", "lịch sử điểm"),
            Map.entry("promotion_customer", "khuyến mãi gán riêng"),
            Map.entry("promotion_usage", "lượt dùng khuyến mãi"),
            Map.entry("bug_report", "báo lỗi"),
            Map.entry("customer_profile", "khách được giới thiệu")
    );

    private MergeLogWriter() {
    }

    public static int insert(Connection con, String type, Integer keptCustomerId, Collection<Integer> mergedCustomerIds,
                             Integer keptVehicleId, Collection<Integer> mergedVehicleIds, String snapshotJson,
                             String summary, String note, Integer staffId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO customer_merge_log (merge_type, kept_customer_id, merged_customer_ids, kept_vehicle_id,"
                        + " merged_vehicle_ids, snapshot_json, summary, note, merged_by_staff_id, merged_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, type);
            ps.setObject(2, keptCustomerId);
            ps.setString(3, joinIds(mergedCustomerIds));
            ps.setObject(4, keptVehicleId);
            ps.setString(5, joinIds(mergedVehicleIds));
            ps.setString(6, snapshotJson);
            ps.setString(7, summary);
            ps.setString(8, truncate(note, 500));
            ps.setObject(9, staffId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public static String labelOf(String table) {
        return TABLE_LABELS.getOrDefault(table.toLowerCase(), table);
    }

    public static String describe(List<ReferenceRewriter.RewriteResult> rewrites) {
        return rewrites.stream()
                .filter(r -> r.moved() > 0 || r.droppedAsDuplicate() > 0)
                .map(r -> {
                    String label = labelOf(r.ref().table());
                    String text = r.moved() + " " + label;
                    if (r.droppedAsDuplicate() > 0) text += " (bỏ " + r.droppedAsDuplicate() + " dòng trùng)";
                    return text;
                })
                .collect(Collectors.joining(", "));
    }

    private static String joinIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
