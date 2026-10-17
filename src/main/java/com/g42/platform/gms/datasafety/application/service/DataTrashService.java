package com.g42.platform.gms.datasafety.application.service;

import com.g42.platform.gms.datasafety.application.service.TrashRegistry.TrashType;
import com.g42.platform.gms.warehouse.app.service.catalog.CatalogItemService;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Thùng rác dữ liệu: đọc các dòng đang bị xoá mềm theo {@link TrashRegistry} và
 * khôi phục chúng. Không có "xoá vĩnh viễn" cho dữ liệu nghiệp vụ — dòng đã xoá mềm
 * vẫn bị lịch sử trỏ tới (dòng báo giá trỏ mục kho, hồ sơ ứng viên trỏ tin tuyển
 * dụng...), xoá cứng là gãy khoá ngoại hoặc mất lịch sử. Xoá vĩnh viễn chỉ có ở ảnh.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataTrashService {

    private static final int MAX_PAGE_SIZE = 100;

    private final JdbcTemplate jdbcTemplate;
    private final CatalogItemService catalogItemService;

    public List<TrashTypeSummary> summary() {
        List<TrashTypeSummary> result = new ArrayList<>();
        for (TrashType type : TrashRegistry.all()) {
            Long count;
            try {
                count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) " + type.fromSql() + " WHERE " + type.deletedWhere(), Long.class);
            } catch (DataAccessException exception) {
                // Một bảng lỗi (VD DB chưa chạy migration của module đó) không được làm sập cả thùng rác.
                log.warn("Không đếm được thùng rác {}: {}", type.key(), exception.getMostSpecificCause().getMessage());
                count = null;
            }
            result.add(new TrashTypeSummary(type.key(), type.group(), type.label(),
                    count, type.imageExpr() != null, type.restoreNote()));
        }
        return result;
    }

    public TrashPage list(String key, String search, int page, int size) {
        TrashType type = requireType(key);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int pageIndex = Math.max(page, 0);

        String where = " WHERE " + type.deletedWhere();
        List<Object> params = new ArrayList<>();
        String keyword = search == null ? "" : search.trim();
        if (!keyword.isEmpty()) {
            where += " AND " + type.searchExpr() + " LIKE ?";
            params.add("%" + escapeLike(keyword) + "%");
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) " + type.fromSql() + where, Long.class, params.toArray());

        String sql = "SELECT " + type.idExpr() + " AS id, "
                + type.titleExpr() + " AS title, "
                + orNull(type.subtitleExpr()) + " AS subtitle, "
                + orNull(type.imageExpr()) + " AS image, "
                + orNull(type.deletedAtExpr()) + " AS deleted_at "
                + type.fromSql() + where
                + " ORDER BY " + type.orderExpr()
                + " LIMIT ? OFFSET ?";
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add(pageIndex * pageSize);

        List<TrashItem> items = jdbcTemplate.query(sql, (rs, i) -> {
            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            return new TrashItem(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("subtitle"),
                    rs.getString("image"),
                    deletedAt == null ? null : deletedAt.toLocalDateTime().toString());
        }, pageParams.toArray());

        return new TrashPage(type.key(), type.label(), type.restoreNote(), items, total == null ? 0 : total, pageIndex, pageSize);
    }

    @Transactional
    public RestoreResult restore(String key, long id) {
        TrashType type = requireType(key);
        Integer inTrash = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " + type.fromSql() + " WHERE " + type.deletedWhere() + " AND " + type.idExpr() + " = ?",
                Integer.class, id);
        if (inTrash == null || inTrash == 0) {
            throw new TrashNotFoundException("Bản ghi không còn trong thùng rác (có thể đã được khôi phục)");
        }

        try {
            if (TrashRegistry.CATALOG_ITEM.equals(type.key())) {
                // Đi qua service kho: chặn trùng SKU với mục đang dùng + bật lại bài bán hàng đi kèm.
                catalogItemService.activateCatalogItem(Math.toIntExact(id));
            } else {
                int updated = jdbcTemplate.update(type.restoreSql(), id);
                if (updated == 0) throw new TrashNotFoundException("Bản ghi không còn trong thùng rác");
            }
        } catch (WarehouseException exception) {
            throw new TrashConflictException(exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            throw new TrashConflictException("Không khôi phục được vì trùng với dữ liệu đang dùng: "
                    + exception.getMostSpecificCause().getMessage());
        }

        String title = jdbcTemplate.query(
                "SELECT " + type.titleExpr() + " " + type.fromSql() + " WHERE " + type.idExpr() + " = ?",
                rs -> rs.next() ? rs.getString(1) : null, id);
        log.info("Đã khôi phục {} #{} ({}) từ thùng rác", type.key(), id, title);
        return new RestoreResult(type.key(), type.label(), id, title);
    }

    private static TrashType requireType(String key) {
        TrashType type = TrashRegistry.find(key);
        if (type == null) throw new IllegalArgumentException("Loại dữ liệu không hợp lệ: " + key);
        return type;
    }

    private static String orNull(String expression) {
        return expression == null ? "NULL" : expression;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public record TrashTypeSummary(String key, String group, String label, Long count, boolean hasImage, String restoreNote) {}

    public record TrashItem(long id, String title, String subtitle, String imageUrl, String deletedAt) {}

    public record TrashPage(String key, String label, String restoreNote, List<TrashItem> items, long total, int page, int size) {}

    public record RestoreResult(String key, String label, long id, String title) {}

    public static class TrashNotFoundException extends RuntimeException {
        public TrashNotFoundException(String message) {
            super(message);
        }
    }

    public static class TrashConflictException extends RuntimeException {
        public TrashConflictException(String message) {
            super(message);
        }
    }
}
