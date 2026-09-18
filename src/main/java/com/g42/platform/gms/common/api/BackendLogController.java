package com.g42.platform.gms.common.api;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.common.logging.InMemoryLogAppender;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * API cho quản trị viên xem log ứng dụng backend (ring buffer trong bộ nhớ).
 * Hỗ trợ poll tăng dần bằng afterId để trang FE tự refresh nhẹ nhàng.
 */
@RestController
@RequestMapping("/api/admin/backend-logs")
@PreAuthorize("hasAuthority('" + PermissionCodes.BACKEND_LOG_VIEW + "')")
public class BackendLogController {

    private static final int DEFAULT_LIMIT = 500;
    private static final int MAX_LIMIT = 2000;

    /** Thứ tự nặng dần; dùng để lọc "từ mức này trở lên". */
    private static final List<String> SEVERITY = List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR");

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBackendLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long afterId,
            @RequestParam(required = false) Integer limit) {

        int effectiveLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        String levelFilter = level == null ? "" : level.trim().toUpperCase(Locale.ROOT);
        String searchFilter = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        int minSeverity = SEVERITY.indexOf(levelFilter);

        // Lọc WARN/ERROR thì đọc buffer lỗi riêng: buffer chung chỉ giữ 2000 dòng gần
        // nhất nên một đợt log DEBUG là đủ đẩy lỗi ra ngoài trước khi kịp mở trang.
        List<InMemoryLogAppender.LogEntry> source = minSeverity >= SEVERITY.indexOf("WARN")
                ? InMemoryLogAppender.problemSnapshot()
                : InMemoryLogAppender.snapshot();

        long lastId = source.isEmpty() ? (afterId != null ? afterId : 0) : source.get(source.size() - 1).id();

        List<InMemoryLogAppender.LogEntry> filtered = source.stream()
                .filter(entry -> afterId == null || entry.id() > afterId)
                .filter(entry -> minSeverity < 0 || SEVERITY.indexOf(entry.level()) >= minSeverity)
                .filter(entry -> searchFilter.isEmpty()
                        || entry.message().toLowerCase(Locale.ROOT).contains(searchFilter)
                        || entry.logger().toLowerCase(Locale.ROOT).contains(searchFilter))
                .toList();

        // Giữ các log MỚI nhất khi vượt limit
        if (filtered.size() > effectiveLimit) {
            filtered = filtered.subList(filtered.size() - effectiveLimit, filtered.size());
        }

        return ResponseEntity.ok(ApiResponses.success(Map.of(
                "logs", filtered,
                "lastId", lastId
        )));
    }
}
