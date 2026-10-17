package com.g42.platform.gms.datasafety.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.datasafety.application.service.DatabaseDumpService;
import com.g42.platform.gms.datasafety.application.service.DatabaseDumpService.BackupBusyException;
import com.g42.platform.gms.datasafety.application.service.DatabaseDumpService.DatabaseSummary;
import com.g42.platform.gms.datasafety.application.service.DatabaseDumpService.DumpLease;
import com.g42.platform.gms.datasafety.application.service.DatabaseDumpService.DumpResult;
import com.g42.platform.gms.systemlog.service.AuditService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Màn /data-backup, phần sao lưu. Chỉ ADMIN và MANAGER — mã quyền ở đây bị khoá
 * theo vai trò (PermissionCodes.ROLE_RESTRICTED), tick cho vai trò khác cũng vô hiệu.
 *
 * <p>Mỗi lần tải đều ghi system_log mức CRITICAL kèm kích thước file: bản sao lưu
 * là toàn bộ dữ liệu khách, sau này cần trả lời được "ai đã mang CSDL ra ngoài".
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/data-backup")
public class DataBackupController {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String AUDIT_MODULE = "DATA_BACKUP";

    private final DatabaseDumpService dumpService;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DATA_SAFETY_VIEW + "')")
    public ResponseEntity<ApiResponse<BackupOverview>> summary() {
        DatabaseSummary database = dumpService.summary();
        List<BackupHistoryEntry> history = jdbcTemplate.query("""
                        SELECT actor_name, actor_role, description, ip_address, severity, created_at
                        FROM system_log
                        WHERE module = ? AND action = 'EXPORT'
                        ORDER BY created_at DESC, log_id DESC
                        LIMIT 15
                        """,
                (rs, i) -> {
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    return new BackupHistoryEntry(
                            rs.getString("actor_name"),
                            rs.getString("actor_role"),
                            rs.getString("description"),
                            rs.getString("ip_address"),
                            !"CRITICAL".equals(rs.getString("severity")),
                            createdAt == null ? null : createdAt.toLocalDateTime().toString());
                },
                AUDIT_MODULE);
        return ResponseEntity.ok(ApiResponses.success(new BackupOverview(database, history)));
    }

    /**
     * Tải bản sao lưu .sql.gz. Ghi thẳng ra response (không qua StreamingResponseBody)
     * để không dính giới hạn thời gian của request bất đồng bộ khi CSDL lớn dần.
     */
    @GetMapping("/download")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DATA_BACKUP_EXPORT + "')")
    public void download(HttpServletResponse response) throws IOException {
        DumpLease lease;
        try {
            lease = dumpService.begin();
        } catch (BackupBusyException exception) {
            writeJsonError(response, HttpStatus.CONFLICT, "BACKUP_BUSY", exception.getMessage());
            return;
        }

        String filename = "michelin-gms-backup-" + LocalDateTime.now().format(FILE_STAMP) + ".sql.gz";
        try (lease) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/gzip");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setHeader("Cache-Control", "no-store");
            CountingOutputStream counting = new CountingOutputStream(response.getOutputStream());
            DumpResult result = lease.writeGzip(counting);
            counting.flush();
            response.flushBuffer();

            audit("CRITICAL", String.format("Tải bản sao lưu CSDL %s: %d bảng, %d dòng, %s nén, %.1f giây",
                    filename, result.tableCount(), result.rowCount(), humanBytes(counting.count()),
                    result.elapsedMs() / 1000.0));
            log.info("Đã xuất bản sao lưu {} ({} bảng, {} dòng, {} byte, {} ms)",
                    filename, result.tableCount(), result.rowCount(), counting.count(), result.elapsedMs());
        } catch (Exception exception) {
            log.error("Xuất bản sao lưu {} thất bại", filename, exception);
            audit("WARNING", "Tải bản sao lưu CSDL thất bại: " + exception.getMessage());
            if (!response.isCommitted()) {
                response.reset();
                writeJsonError(response, HttpStatus.INTERNAL_SERVER_ERROR, "BACKUP_FAILED",
                        "Không tạo được bản sao lưu: " + exception.getMessage());
                return;
            }
            // Đã gửi một phần file: ném tiếp để kết nối bị cắt — trình duyệt báo lỗi thay vì lưu file thiếu.
            throw exception instanceof IOException io ? io : new IOException(exception);
        }
    }

    private void audit(String severity, String description) {
        auditService.record(auditService.currentContext()
                .action("EXPORT")
                .module(AUDIT_MODULE)
                .severity(severity)
                .targetType("DATABASE")
                .description(description)
                .build());
    }

    private void writeJsonError(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ApiResponses.error(code, message));
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /** Đếm số byte thật sự đã gửi đi (sau nén) để ghi vào nhật ký. */
    private static final class CountingOutputStream extends FilterOutputStream {
        private long count;

        CountingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            count++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            count += len;
        }

        @Override
        public void close() {
            // Không đóng stream của response — để container tự kết thúc.
        }

        long count() {
            return count;
        }
    }

    /** {@code failed} = lần tải đó lỗi giữa chừng (nhật ký mức WARNING). */
    public record BackupHistoryEntry(String actorName, String actorRole, String description,
                                     String ipAddress, boolean failed, String createdAt) {}

    public record BackupOverview(DatabaseSummary database, List<BackupHistoryEntry> history) {}
}
