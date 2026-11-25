package com.g42.platform.gms.systemlog.service;

import com.g42.platform.gms.systemlog.dto.SystemLogDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nhận ra đối tượng của từng dòng nhật ký (mặt hàng, phiếu nhập/xuất, phiếu dịch vụ, báo giá)
 * từ đường dẫn API trong mô tả, rồi:
 * <ul>
 *   <li>gắn tên dễ đọc (tên mặt hàng + SKU, mã phiếu) — nhật ký cũ chỉ có "update/1306";</li>
 *   <li>cho tìm nhật ký cũ theo tên mặt hàng / mã phiếu (đổi chữ tìm kiếm thành danh sách id);</li>
 *   <li>cho lọc "toàn bộ lịch sử của một đối tượng".</li>
 * </ul>
 * Tra lúc đọc, chỉ cho một trang (≤ 100 dòng), mỗi loại đối tượng một câu IN, nên áp dụng
 * được cho cả nhật ký đã ghi từ trước mà không sửa dữ liệu cũ. Đối tượng đã bị xoá hẳn thì để trống.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemLogTargetResolver {

    public enum Kind {
        CATALOG_ITEM(
                """
                SELECT item_id AS id,
                       CONCAT(item_name, CASE WHEN sku IS NULL OR sku = '' THEN '' ELSE CONCAT(' (SKU ', sku, ')') END) AS label
                FROM catalog_item WHERE item_id IN (:ids)""",
                "SELECT item_id FROM catalog_item WHERE LOWER(item_name) LIKE :q OR LOWER(sku) LIKE :q LIMIT 200",
                List.of("/api/warehouse/catalog-item/", "/api/warehouse/item-color/", "/api/service/create/", "/api/service/combo/"),
                "/api/warehouse/(?:catalog-item/update|catalog-item|item-color)/(\\d+)",
                "/api/service/(?:create|combo)/(\\d+)"),
        STOCK_ENTRY(
                "SELECT entry_id AS id, CONCAT('Phiếu nhập ', entry_code) AS label FROM stock_entry WHERE entry_id IN (:ids)",
                "SELECT entry_id FROM stock_entry WHERE LOWER(entry_code) LIKE :q LIMIT 200",
                List.of("/api/warehouse/stock-entries/"),
                "/api/warehouse/stock-entries/(\\d+)"),
        STOCK_ISSUE(
                "SELECT issue_id AS id, CONCAT('Phiếu xuất ', issue_code) AS label FROM stock_issue WHERE issue_id IN (:ids)",
                "SELECT issue_id FROM stock_issue WHERE LOWER(issue_code) LIKE :q LIMIT 200",
                List.of("/api/warehouse/stock-issues/"),
                "/api/warehouse/stock-issues/(\\d+)"),
        ESTIMATE(
                """
                SELECT e.estimate_id AS id, CONCAT('Báo giá của phiếu ', COALESCE(t.ticket_code, CONCAT('#', e.service_ticket_id))) AS label
                FROM estimate e LEFT JOIN service_ticket t ON t.service_ticket_id = e.service_ticket_id
                WHERE e.estimate_id IN (:ids)""",
                """
                SELECT e.estimate_id FROM estimate e JOIN service_ticket t ON t.service_ticket_id = e.service_ticket_id
                WHERE LOWER(t.ticket_code) LIKE :q LIMIT 200""",
                List.of("/api/service-ticket/estimate/"),
                "/api/service-ticket/estimate/(\\d+)"),
        SERVICE_TICKET(
                "SELECT service_ticket_id AS id, CONCAT('Phiếu ', ticket_code) AS label FROM service_ticket WHERE service_ticket_id IN (:ids)",
                "SELECT service_ticket_id FROM service_ticket WHERE LOWER(ticket_code) LIKE :q LIMIT 200",
                List.of("/api/service-ticket/manage/", "/api/service-ticket/parts-sale/", "/api/payment/proofs/",
                        "/api/warehouse/allocations/"),
                "/api/service-ticket/manage/(\\d+)/",
                "/api/service-ticket/parts-sale/(\\d+)/",
                "/api/payment/proofs/(\\d+)",
                "/api/warehouse/allocations/(\\d+)/request-issue");

        final String labelSql;
        final String searchSql;
        /** Đoạn đường dẫn để lọc SQL theo loại (kèm target_id = id). */
        public final List<String> pathFragments;
        final List<Pattern> patterns;

        Kind(String labelSql, String searchSql, List<String> pathFragments, String... pathRegexes) {
            this.labelSql = labelSql;
            this.searchSql = searchSql;
            this.pathFragments = pathFragments;
            List<Pattern> compiled = new ArrayList<>();
            for (String regex : pathRegexes) {
                // Mô tả cũ bắt đầu bằng "PUT /api/...", mô tả mới có dòng cuối "API: PUT /api/..."
                compiled.add(Pattern.compile("\\b(?:GET|POST|PUT|PATCH|DELETE) " + regex));
            }
            this.patterns = compiled;
        }

        public static Kind parse(String code) {
            if (code == null || code.isBlank()) return null;
            try {
                return Kind.valueOf(code.trim());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** Thứ tự kiểm tra: báo giá trước phiếu (estimate/123/stock-allocation vẫn là báo giá). */
    private static final List<Kind> MATCH_ORDER = List.of(
            Kind.CATALOG_ITEM, Kind.STOCK_ENTRY, Kind.STOCK_ISSUE, Kind.ESTIMATE, Kind.SERVICE_TICKET);

    private final NamedParameterJdbcTemplate jdbc;

    private record Wanted(SystemLogDto row, Kind kind, Integer id) {
    }

    /** Gắn targetKind/targetName cho các dòng của một trang nhật ký. */
    public void fill(List<SystemLogDto> logs) {
        if (logs == null || logs.isEmpty()) return;
        try {
            List<Wanted> wanted = new ArrayList<>();
            Map<Kind, Set<Integer>> idsByKind = new EnumMap<>(Kind.class);
            for (SystemLogDto row : logs) {
                Wanted ref = match(row);
                if (ref == null) continue;
                row.setTargetKind(ref.kind().name());
                row.setTargetId(String.valueOf(ref.id()));
                wanted.add(ref);
                idsByKind.computeIfAbsent(ref.kind(), k -> new LinkedHashSet<>()).add(ref.id());
            }
            if (wanted.isEmpty()) return;

            Map<Kind, Map<Integer, String>> labels = new EnumMap<>(Kind.class);
            idsByKind.forEach((kind, ids) -> {
                Map<Integer, String> byId = new HashMap<>();
                jdbc.query(kind.labelSql, new MapSqlParameterSource("ids", ids),
                        (RowCallbackHandler) rs -> byId.put(rs.getInt("id"), rs.getString("label")));
                labels.put(kind, byId);
            });

            for (Wanted ref : wanted) {
                String label = labels.getOrDefault(ref.kind(), Map.of()).get(ref.id());
                if (label != null) ref.row().setTargetName(label);
            }
        } catch (Exception e) {
            // Thiếu tên chỉ làm nhật ký kém dễ đọc, không được làm hỏng cả trang
            log.warn("Không tra được tên đối tượng cho nhật ký: {}", e.getMessage());
        }
    }

    /**
     * Chữ tìm kiếm → id mặt hàng / phiếu có tên hoặc mã chứa chữ đó, để tìm được cả
     * nhật ký cũ (chỉ lưu id). Chữ quá ngắn thì bỏ qua cho đỡ tốn.
     */
    public Map<Kind, List<Integer>> idsMatching(String search) {
        Map<Kind, List<Integer>> result = new EnumMap<>(Kind.class);
        if (search == null || search.trim().length() < 3) return result;
        MapSqlParameterSource params = new MapSqlParameterSource("q", "%" + search.trim().toLowerCase() + "%");
        for (Kind kind : Kind.values()) {
            try {
                List<Integer> ids = jdbc.queryForList(kind.searchSql, params, Integer.class);
                if (!ids.isEmpty()) result.put(kind, ids);
            } catch (Exception e) {
                log.warn("Không tìm được {} cho nhật ký: {}", kind, e.getMessage());
            }
        }
        return result;
    }

    private static Wanted match(SystemLogDto row) {
        String description = row.getDescription();
        if (description == null || description.isBlank()) return null;
        for (Kind kind : MATCH_ORDER) {
            for (Pattern pattern : kind.patterns) {
                Matcher m = pattern.matcher(description);
                if (m.find()) {
                    try {
                        return new Wanted(row, kind, Integer.parseInt(m.group(1)));
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
}
