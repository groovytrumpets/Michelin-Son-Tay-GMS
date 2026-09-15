package com.g42.platform.gms.customermerge.infrastructure.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Đọc nguyên văn các dòng sắp bị gộp/xoá để lưu vào customer_merge_log.snapshot_json.
 * Giá trị ngày giờ/số thập phân đổi sang chuỗi để JSON không phụ thuộc module Jackson nào.
 */
public final class RowSnapshots {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RowSnapshots() {
    }

    public static List<Map<String, Object>> read(Connection con, String table, String idColumn,
                                                 Collection<Integer> ids) throws SQLException {
        if (ids.isEmpty()) return List.of();
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM " + ReferenceRewriter.quote(table) + " WHERE " + ReferenceRewriter.quote(idColumn)
                        + " IN (" + ReferenceRewriter.placeholders(ids.size()) + ")")) {
            ReferenceRewriter.bind(ps, 1, ids);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        row.put(meta.getColumnLabel(i), plain(rs.getObject(i)));
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    /** Bản sao một dòng đã đọc sẵn (JdbcTemplate), đổi giá trị lạ sang chuỗi; bỏ các cột nhạy cảm. */
    public static Map<String, Object> plainRow(Map<String, Object> row, String... omitColumns) {
        Map<String, Object> copy = new LinkedHashMap<>();
        java.util.Set<String> omit = java.util.Set.of(omitColumns);
        row.forEach((column, value) -> {
            if (!omit.contains(column)) copy.put(column, plain(value));
        });
        return copy;
    }

    private static Object plain(Object value) {
        if (value == null || value instanceof String || value instanceof Number && !(value instanceof BigDecimal)
                || value instanceof Boolean) {
            return value;
        }
        return value.toString();
    }
}
