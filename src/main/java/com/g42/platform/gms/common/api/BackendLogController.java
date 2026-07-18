package com.g42.platform.gms.common.api;

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
@PreAuthorize("hasRole('ADMIN')")
public class BackendLogController {

    private static final int DEFAULT_LIMIT = 500;
    private static final int MAX_LIMIT = 2000;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBackendLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long afterId,
            @RequestParam(required = false) Integer limit) {

        int effectiveLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        String levelFilter = level == null ? "" : level.trim().toUpperCase(Locale.ROOT);
        String searchFilter = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        List<InMemoryLogAppender.LogEntry> all = InMemoryLogAppender.snapshot();
        long lastId = all.isEmpty() ? (afterId != null ? afterId : 0) : all.get(all.size() - 1).id();

        List<InMemoryLogAppender.LogEntry> filtered = all.stream()
                .filter(entry -> afterId == null || entry.id() > afterId)
                .filter(entry -> levelFilter.isEmpty() || levelFilter.equals(entry.level()))
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
