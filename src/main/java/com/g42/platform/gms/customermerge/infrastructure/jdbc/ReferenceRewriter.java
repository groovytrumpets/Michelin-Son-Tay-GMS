package com.g42.platform.gms.customermerge.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Chuyển mọi dòng đang trỏ tới id cũ (khách/xe bị gộp) sang id được giữ lại.
 *
 * Danh sách bảng KHÔNG viết cứng mà dò trong information_schema theo tên cột: hệ thống
 * có hàng chục bảng lưu customer_id / vehicle_id, nhiều bảng không có khoá ngoại, và DB
 * các môi trường không giống nhau (bảng nào có ở đây chưa chắc có ở kia). Viết cứng thì
 * thêm bảng mới là quên, để lại dữ liệu mồ côi trỏ vào hồ sơ đã xoá.
 *
 * Plain JDBC, không phụ thuộc Spring — changeset Liquibase 037-3 dùng chung.
 */
public final class ReferenceRewriter {

    private ReferenceRewriter() {
    }

    /** Một cột tham chiếu; uniqueIndexed = cột nằm trong một UNIQUE index nên chuyển có thể đụng trùng. */
    public record RefColumn(String table, String column, boolean uniqueIndexed) {
    }

    /** Kết quả chuyển một cột: số dòng đã chuyển và số dòng bị bỏ vì bên giữ lại đã có dòng tương đương. */
    public record RewriteResult(RefColumn ref, int moved, int droppedAsDuplicate) {
    }

    public static List<RefColumn> discover(Connection con, Collection<String> columnNames,
                                           Collection<String> excludeTables) throws SQLException {
        if (columnNames.isEmpty()) return List.of();
        Set<String> excluded = excludeTables.stream()
                .map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        String placeholders = placeholders(columnNames.size());

        Set<String> uniqueColumns = new HashSet<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT DISTINCT TABLE_NAME, COLUMN_NAME FROM information_schema.STATISTICS"
                        + " WHERE TABLE_SCHEMA = DATABASE() AND NON_UNIQUE = 0 AND COLUMN_NAME IN (" + placeholders + ")")) {
            bind(ps, 1, columnNames);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) uniqueColumns.add(key(rs.getString(1), rs.getString(2)));
            }
        }

        List<RefColumn> result = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT c.TABLE_NAME, c.COLUMN_NAME FROM information_schema.COLUMNS c"
                        + " JOIN information_schema.TABLES t ON t.TABLE_SCHEMA = c.TABLE_SCHEMA AND t.TABLE_NAME = c.TABLE_NAME"
                        + " WHERE c.TABLE_SCHEMA = DATABASE() AND t.TABLE_TYPE = 'BASE TABLE'"
                        + " AND c.COLUMN_NAME IN (" + placeholders + ")"
                        + " ORDER BY c.TABLE_NAME, c.COLUMN_NAME")) {
            bind(ps, 1, columnNames);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String table = rs.getString(1);
                    String column = rs.getString(2);
                    if (excluded.contains(table.toLowerCase(Locale.ROOT))) continue;
                    result.add(new RefColumn(table, column, uniqueColumns.contains(key(table, column))));
                }
            }
        }
        return result;
    }

    /**
     * Chuyển fromIds → toId trên một cột.
     *
     * Cột thuộc UNIQUE index (VD mỗi khách một dòng): dòng nào chuyển sang sẽ trùng với dòng
     * bên giữ lại thì bỏ, vì bên giữ lại đã có bản tương đương. UPDATE IGNORE ở đây chỉ có
     * thể vướng trùng khoá — id đích chắc chắn tồn tại nên không vướng khoá ngoại.
     */
    public static RewriteResult rewrite(Connection con, RefColumn ref, int toId,
                                        Collection<Integer> fromIds) throws SQLException {
        if (fromIds.isEmpty()) return new RewriteResult(ref, 0, 0);
        String table = quote(ref.table());
        String column = quote(ref.column());
        String in = placeholders(fromIds.size());

        int moved;
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE " + (ref.uniqueIndexed() ? "IGNORE " : "") + table
                        + " SET " + column + " = ? WHERE " + column + " IN (" + in + ")")) {
            ps.setInt(1, toId);
            bind(ps, 2, fromIds);
            moved = ps.executeUpdate();
        }

        int dropped = 0;
        if (ref.uniqueIndexed()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM " + table + " WHERE " + column + " IN (" + in + ")")) {
                bind(ps, 1, fromIds);
                dropped = ps.executeUpdate();
            }
        }
        return new RewriteResult(ref, moved, dropped);
    }

    public static int count(Connection con, RefColumn ref, Collection<Integer> ids) throws SQLException {
        if (ids.isEmpty()) return 0;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM " + quote(ref.table()) + " WHERE " + quote(ref.column())
                        + " IN (" + placeholders(ids.size()) + ")")) {
            bind(ps, 1, ids);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public static String placeholders(int n) {
        return String.join(",", java.util.Collections.nCopies(n, "?"));
    }

    public static void bind(PreparedStatement ps, int startIndex, Collection<?> values) throws SQLException {
        int i = startIndex;
        for (Object value : values) ps.setObject(i++, value);
    }

    /** Tên bảng/cột lấy từ information_schema, vẫn bọc backtick để không vướng từ khoá MySQL. */
    public static String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private static String key(String table, String column) {
        return table.toLowerCase(Locale.ROOT) + "." + column.toLowerCase(Locale.ROOT);
    }
}
