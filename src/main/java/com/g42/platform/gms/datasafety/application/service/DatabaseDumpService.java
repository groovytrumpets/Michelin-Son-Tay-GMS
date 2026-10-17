package com.g42.platform.gms.datasafety.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * Xuất toàn bộ CSDL thành một file SQL (nén gzip), tương đương
 * {@code mysqldump --single-transaction --hex-blob}.
 *
 * <p>Tự viết bằng JDBC thay vì gọi {@code mysqldump} vì backend chạy ở nhiều nơi
 * (IntelliJ trên máy dev, container trên VPS) mà không nơi nào bảo đảm có sẵn
 * binary đó, đúng phiên bản MySQL 9 của server, và mật khẩu DB không phải lộ ra
 * dòng lệnh của tiến trình con.
 *
 * <p>Tính nhất quán: cả bản sao lưu đọc trong MỘT transaction
 * {@code WITH CONSISTENT SNAPSHOT}, nên phiếu ghi giữa chừng không làm lệch giữa
 * bảng này với bảng khác — và không khoá bảng nào, xưởng vẫn làm việc bình thường.
 *
 * <p>Không lưu file trên máy chủ: ghi thẳng ra response. Mỗi lần chỉ cho chạy một
 * bản ({@link #begin()}) để hai người cùng bấm không nhân đôi tải lên DB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseDumpService {

    /** Mỗi câu INSERT gộp nhiều dòng tới khoảng ngần này ký tự — xa dưới max_allowed_packet mặc định (64MB). */
    private static final int INSERT_BATCH_CHARS = 512 * 1024;
    private static final Pattern VIEW_DEFINER = Pattern.compile("DEFINER=`[^`]*`@`[^`]*`\\s*");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Set<String> NUMERIC_TYPES = Set.of(
            "tinyint", "smallint", "mediumint", "int", "integer", "bigint",
            "decimal", "numeric", "float", "double", "real");
    private static final Set<String> TEMPORAL_TYPES = Set.of("date", "datetime", "timestamp", "time", "year");
    private static final Set<String> BINARY_TYPES = Set.of(
            "binary", "varbinary", "tinyblob", "blob", "mediumblob", "longblob",
            "geometry", "point", "linestring", "polygon", "multipoint", "multilinestring",
            "multipolygon", "geometrycollection", "geomcollection", "vector");

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // ------------------------------------------------------------------ tổng quan

    public DatabaseSummary summary() {
        String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        List<TableStat> tables = jdbcTemplate.query("""
                        SELECT TABLE_NAME, TABLE_TYPE, COALESCE(TABLE_ROWS, 0) AS row_count,
                               COALESCE(DATA_LENGTH, 0) + COALESCE(INDEX_LENGTH, 0) AS size_bytes
                        FROM information_schema.TABLES
                        WHERE TABLE_SCHEMA = DATABASE()
                        ORDER BY size_bytes DESC, TABLE_NAME
                        """,
                (rs, i) -> new TableStat(
                        rs.getString("TABLE_NAME"),
                        "VIEW".equalsIgnoreCase(rs.getString("TABLE_TYPE")),
                        rs.getLong("row_count"),
                        rs.getLong("size_bytes")));
        long rows = tables.stream().mapToLong(TableStat::approxRows).sum();
        long bytes = tables.stream().mapToLong(TableStat::sizeBytes).sum();
        int tableCount = (int) tables.stream().filter(t -> !t.view()).count();
        return new DatabaseSummary(databaseName, version, tableCount, rows, bytes, tables, running.get());
    }

    // ------------------------------------------------------------------ xuất file

    /**
     * Giữ chỗ chạy sao lưu. Gọi TRƯỚC khi ghi header của response, để lúc bận còn
     * trả được 409 dạng JSON bình thường.
     */
    public DumpLease begin() {
        if (!running.compareAndSet(false, true)) {
            throw new BackupBusyException("Đang có một bản sao lưu khác được tạo, vui lòng thử lại sau ít phút");
        }
        return new DumpLease();
    }

    public final class DumpLease implements AutoCloseable {
        private boolean closed;

        private DumpLease() {
        }

        /** Ghi bản sao lưu dạng gzip vào {@code out}. Không đóng {@code out}. */
        public DumpResult writeGzip(OutputStream out) throws IOException, SQLException {
            GZIPOutputStream gzip = new GZIPOutputStream(out, 64 * 1024);
            Writer writer = new BufferedWriter(new OutputStreamWriter(gzip, StandardCharsets.UTF_8), 256 * 1024);
            DumpResult result = writeSql(writer);
            writer.flush();
            gzip.finish();
            return result;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                running.set(false);
            }
        }
    }

    private DumpResult writeSql(Writer out) throws IOException, SQLException {
        long startedAt = System.currentTimeMillis();
        try (Connection connection = dataSource.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            int previousIsolation = connection.getTransactionIsolation();
            String previousTimeZone = queryString(connection, "SELECT @@session.time_zone");
            try {
                connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement()) {
                    // TIMESTAMP được server đổi theo time_zone của phiên lúc trả về. Đọc ở +00:00 và
                    // ghi "SET TIME_ZONE='+00:00'" vào đầu file thì khôi phục ở đâu cũng ra đúng thời điểm.
                    statement.execute("SET SESSION time_zone = '+00:00'");
                    statement.execute("START TRANSACTION WITH CONSISTENT SNAPSHOT, READ ONLY");
                }
                DumpResult result = writeSnapshot(connection, out, startedAt);
                connection.commit();
                return result;
            } catch (IOException | SQLException | RuntimeException exception) {
                safeRollback(connection);
                throw exception;
            } finally {
                // Kết nối quay về pool — trả lại đúng trạng thái phiên cũ cho người dùng kế tiếp.
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET SESSION time_zone = " + quote(previousTimeZone == null ? "SYSTEM" : previousTimeZone));
                } catch (SQLException ignored) {
                    // kết nối hỏng thì pool tự loại
                }
                connection.setAutoCommit(previousAutoCommit);
                connection.setTransactionIsolation(previousIsolation);
            }
        }
    }

    private DumpResult writeSnapshot(Connection connection, Writer out, long startedAt) throws IOException, SQLException {
        String databaseName = queryString(connection, "SELECT DATABASE()");
        String version = queryString(connection, "SELECT VERSION()");

        Map<String, String> tableTypes = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("""
                     SELECT TABLE_NAME, TABLE_TYPE FROM information_schema.TABLES
                     WHERE TABLE_SCHEMA = DATABASE() ORDER BY TABLE_NAME""")) {
            while (rs.next()) tableTypes.put(rs.getString(1), rs.getString(2));
        }

        Map<String, List<ColumnInfo>> columnsByTable = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("""
                     SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, EXTRA FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = DATABASE() ORDER BY TABLE_NAME, ORDINAL_POSITION""")) {
            while (rs.next()) {
                String extra = rs.getString(4) == null ? "" : rs.getString(4).toUpperCase(Locale.ROOT);
                // Cột sinh tự động (VD gross_profit STORED GENERATED) không INSERT được — server tự tính lại.
                // DEFAULT_GENERATED chỉ là "có DEFAULT biểu thức", vẫn phải ghi giá trị.
                boolean generated = extra.contains("VIRTUAL GENERATED") || extra.contains("STORED GENERATED");
                columnsByTable.computeIfAbsent(rs.getString(1), k -> new ArrayList<>())
                        .add(new ColumnInfo(rs.getString(2), rs.getString(3).toLowerCase(Locale.ROOT), generated));
            }
        }

        out.write("-- =============================================================\n");
        out.write("-- Michelin Sơn Tây GMS — bản sao lưu toàn bộ cơ sở dữ liệu\n");
        out.write("-- CSDL gốc : " + databaseName + "  (MySQL " + version + ")\n");
        out.write("-- Thời điểm: " + LocalDateTime.now().format(STAMP) + " (giờ máy chủ backend)\n");
        out.write("-- Khôi phục: tạo CSDL trống rồi chạy\n");
        out.write("--     gunzip -c <file>.sql.gz | mysql -u <user> -p <ten_csdl>\n");
        out.write("--   File kết thúc bằng dòng \"-- HOÀN TẤT\"; không thấy dòng đó là file bị cắt ngang.\n");
        out.write("-- CẢNH BÁO: chứa toàn bộ dữ liệu khách hàng và mật khẩu đã mã hoá — giữ kín.\n");
        out.write("-- =============================================================\n\n");
        out.write("SET NAMES utf8mb4;\n");
        out.write("SET TIME_ZONE = '+00:00';\n");
        out.write("SET FOREIGN_KEY_CHECKS = 0;\n");
        out.write("SET UNIQUE_CHECKS = 0;\n");
        out.write("SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';\n");
        out.write("SET AUTOCOMMIT = 0;\n\n");

        int tableCount = 0;
        long rowCount = 0;
        List<String> views = new ArrayList<>();
        for (Map.Entry<String, String> entry : tableTypes.entrySet()) {
            String table = entry.getKey();
            if ("VIEW".equalsIgnoreCase(entry.getValue())) {
                views.add(table);
                continue;
            }
            tableCount++;
            String createSql = showCreate(connection, "SHOW CREATE TABLE " + ident(table), 2);
            out.write("-- -------------------------------------------------------------\n");
            out.write("-- Bảng " + ident(table) + "\n");
            out.write("-- -------------------------------------------------------------\n");
            out.write("DROP TABLE IF EXISTS " + ident(table) + ";\n");
            out.write(createSql + ";\n\n");
            rowCount += writeRows(connection, out, table, columnsByTable.getOrDefault(table, List.of()));
            out.write("COMMIT;\n\n");
        }

        for (String view : views) {
            String createSql = showCreate(connection, "SHOW CREATE VIEW " + ident(view), 2);
            out.write("-- View " + ident(view) + "\n");
            out.write("DROP VIEW IF EXISTS " + ident(view) + ";\n");
            // DEFINER trỏ tới user của máy gốc, máy khôi phục thường không có user đó.
            out.write(VIEW_DEFINER.matcher(createSql).replaceAll("") + ";\n\n");
        }

        long elapsedMs = System.currentTimeMillis() - startedAt;
        out.write("SET FOREIGN_KEY_CHECKS = 1;\n");
        out.write("SET UNIQUE_CHECKS = 1;\n");
        out.write("SET AUTOCOMMIT = 1;\n");
        out.write("-- HOÀN TẤT: " + tableCount + " bảng, " + views.size() + " view, " + rowCount
                + " dòng, " + elapsedMs + " ms\n");
        return new DumpResult(databaseName, tableCount, views.size(), rowCount, elapsedMs);
    }

    private long writeRows(Connection connection, Writer out, String table, List<ColumnInfo> allColumns)
            throws IOException, SQLException {
        List<ColumnInfo> columns = allColumns.stream().filter(c -> !c.generated()).toList();
        if (columns.isEmpty()) return 0;

        StringBuilder select = new StringBuilder("SELECT ");
        StringBuilder columnList = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo column = columns.get(i);
            if (i > 0) {
                select.append(", ");
                columnList.append(", ");
            }
            select.append(selectExpression(column));
            columnList.append(ident(column.name()));
        }
        select.append(" FROM ").append(ident(table));
        String insertPrefix = "INSERT INTO " + ident(table) + " (" + columnList + ") VALUES\n";

        long rows = 0;
        StringBuilder batch = new StringBuilder();
        // Streaming result set của Connector/J: không nạp cả bảng vào RAM.
        try (Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            statement.setFetchSize(Integer.MIN_VALUE);
            try (ResultSet rs = statement.executeQuery(select.toString())) {
                while (rs.next()) {
                    batch.append(batch.isEmpty() ? insertPrefix : ",\n").append('(');
                    for (int i = 0; i < columns.size(); i++) {
                        if (i > 0) batch.append(',');
                        appendValue(batch, columns.get(i), rs.getString(i + 1));
                    }
                    batch.append(')');
                    rows++;
                    if (batch.length() >= INSERT_BATCH_CHARS) {
                        out.write(batch.append(";\n").toString());
                        batch.setLength(0);
                    }
                }
            }
        }
        if (!batch.isEmpty()) out.write(batch.append(";\n").toString());
        return rows;
    }

    /**
     * Cách đọc từng loại cột sao cho ghi lại được y nguyên. Ép về chuỗi ngay trong SQL
     * để driver không tự diễn giải (múi giờ, kiểu boolean của BIT(1), mã hoá nhị phân).
     */
    private static String selectExpression(ColumnInfo column) {
        String name = ident(column.name());
        String type = column.dataType();
        if (TEMPORAL_TYPES.contains(type)) return "CAST(" + name + " AS CHAR)";
        if ("bit".equals(type)) return "CAST(" + name + " AS UNSIGNED)";
        // TINYINT(1) bị Connector/J coi là boolean — getString có thể ra "true"/"false" hoặc nuốt mất giá trị 2, -1.
        if ("tinyint".equals(type)) return "CAST(" + name + " AS SIGNED)";
        if (BINARY_TYPES.contains(type)) return "HEX(" + name + ")";
        return name;
    }

    private static void appendValue(StringBuilder sb, ColumnInfo column, String value) {
        if (value == null) {
            sb.append("NULL");
            return;
        }
        String type = column.dataType();
        if (NUMERIC_TYPES.contains(type) || "bit".equals(type)) {
            sb.append(value);
        } else if (BINARY_TYPES.contains(type)) {
            if (value.isEmpty()) sb.append("''");
            else sb.append("0x").append(value);
        } else {
            appendQuoted(sb, value);
        }
    }

    /** Thoát chuỗi kiểu mysqldump (dựa trên backslash; file đặt SQL_MODE không có NO_BACKSLASH_ESCAPES). */
    static void appendQuoted(StringBuilder sb, String value) {
        sb.append('\'');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\0' -> sb.append("\\0");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\\' -> sb.append("\\\\");
                case '\'' -> sb.append("\\'");
                case '"' -> sb.append("\\\"");
                case '\u001A' -> sb.append("\\Z");
                default -> sb.append(c);
            }
        }
        sb.append('\'');
    }

    private static String quote(String value) {
        StringBuilder sb = new StringBuilder();
        appendQuoted(sb, value);
        return sb.toString();
    }

    static String ident(String name) {
        return "`" + name.replace("`", "``") + "`";
    }

    private static String showCreate(Connection connection, String sql, int column) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            if (!rs.next()) throw new SQLException("Không đọc được định nghĩa: " + sql);
            return rs.getString(column);
        }
    }

    private static String queryString(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static void safeRollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // không còn gì để làm — pool sẽ tự loại kết nối hỏng
        }
    }

    // ------------------------------------------------------------------ kiểu dữ liệu

    private record ColumnInfo(String name, String dataType, boolean generated) {}

    public record TableStat(String name, boolean view, long approxRows, long sizeBytes) {}

    public record DatabaseSummary(
            String databaseName,
            String serverVersion,
            int tableCount,
            long approxRows,
            long totalBytes,
            List<TableStat> tables,
            boolean backupRunning) {}

    public record DumpResult(String databaseName, int tableCount, int viewCount, long rowCount, long elapsedMs) {}

    public static class BackupBusyException extends RuntimeException {
        public BackupBusyException(String message) {
            super(message);
        }
    }
}
