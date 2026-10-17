package com.g42.platform.gms.datasafety.application.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Thùng rác ảnh của thư viện ảnh (/media-library).
 *
 * <p>Trước đây "Xoá ảnh" gọi destroy trên Cloudinary — mất hẳn. Giờ xoá là đổi loại
 * giao ảnh từ {@code upload} sang {@code private} (giữ nguyên public_id): đường dẫn
 * công khai ngừng hoạt động ngay, ảnh biến khỏi thư viện (thư viện chỉ liệt kê
 * {@code type=upload}), nhưng file vẫn nằm trên Cloudinary để khôi phục. Bảng
 * {@code media_trash} nhớ ai xoá, lúc nào. Xem trước trong thùng rác dùng URL có chữ ký.
 *
 * <p>Ảnh trong thùng rác vẫn tính vào dung lượng Cloudinary cho tới khi xoá vĩnh viễn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaTrashService {

    private static final String LIVE_TYPE = "upload";
    private static final String TRASH_TYPE = "private";
    private static final int MAX_PAGE_SIZE = 100;

    private final Cloudinary cloudinary;
    private final JdbcTemplate jdbcTemplate;

    /** Gọi sau khi thư viện ảnh đã xác nhận ảnh không còn được dùng ở đâu. */
    public void moveToTrash(String publicId, String secureUrl, String folder, String format,
                            Integer width, Integer height, Long bytes) throws IOException {
        cloudinary.uploader().rename(publicId, publicId, Map.of(
                "resource_type", "image",
                "type", LIVE_TYPE,
                "to_type", TRASH_TYPE,
                "invalidate", true));

        Actor actor = currentActor();
        // public_id là duy nhất trong bảng: dòng cũ còn sót (VD khôi phục lỗi giữa chừng) thì ghi đè.
        jdbcTemplate.update("""
                        INSERT INTO media_trash (public_id, original_link, folder, format, width, height, bytes,
                                                 deleted_by, deleted_by_name, deleted_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE original_link = VALUES(original_link), folder = VALUES(folder),
                            format = VALUES(format), width = VALUES(width), height = VALUES(height),
                            bytes = VALUES(bytes), deleted_by = VALUES(deleted_by),
                            deleted_by_name = VALUES(deleted_by_name), deleted_at = VALUES(deleted_at)
                        """,
                publicId, secureUrl, folder, format, width, height, bytes,
                actor.staffId(), actor.name(), Timestamp.valueOf(LocalDateTime.now()));
        log.info("Ảnh {} đã chuyển vào thùng rác bởi {}", publicId, actor.name());
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM media_trash", Long.class);
        return count == null ? 0 : count;
    }

    public MediaTrashPage list(String search, int page, int size) {
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int pageIndex = Math.max(page, 0);
        String keyword = search == null ? "" : search.trim();
        String where = keyword.isEmpty() ? "" : " WHERE CONCAT_WS(' ', public_id, folder, deleted_by_name) LIKE ?";
        List<Object> params = new ArrayList<>();
        if (!keyword.isEmpty()) {
            params.add("%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM media_trash" + where, Long.class, params.toArray());
        Long totalBytes = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(bytes), 0) FROM media_trash", Long.class);

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add(pageIndex * pageSize);
        List<MediaTrashItem> items = jdbcTemplate.query(
                "SELECT * FROM media_trash" + where + " ORDER BY deleted_at DESC, trash_id DESC LIMIT ? OFFSET ?",
                (rs, i) -> {
                    String publicId = rs.getString("public_id");
                    String format = rs.getString("format");
                    Timestamp deletedAt = rs.getTimestamp("deleted_at");
                    return new MediaTrashItem(
                            rs.getLong("trash_id"),
                            publicId,
                            rs.getString("folder"),
                            format,
                            (Integer) rs.getObject("width", Integer.class),
                            (Integer) rs.getObject("height", Integer.class),
                            (Long) rs.getObject("bytes", Long.class),
                            rs.getString("deleted_by_name"),
                            deletedAt == null ? null : deletedAt.toLocalDateTime().toString(),
                            previewUrl(publicId, format));
                },
                pageParams.toArray());
        return new MediaTrashPage(items, total == null ? 0 : total, totalBytes == null ? 0 : totalBytes, pageIndex, pageSize);
    }

    /** Đưa ảnh về lại loại upload — đường dẫn công khai cũ dùng lại được y như trước. */
    public RestoredMedia restore(long trashId) {
        TrashRow row = requireRow(trashId);
        try {
            Map<?, ?> result = cloudinary.uploader().rename(row.publicId(), row.publicId(), Map.of(
                    "resource_type", "image",
                    "type", TRASH_TYPE,
                    "to_type", LIVE_TYPE,
                    "invalidate", true));
            jdbcTemplate.update("DELETE FROM media_trash WHERE trash_id = ?", trashId);
            String secureUrl = Objects.toString(result.get("secure_url"), row.originalLink());
            log.info("Đã khôi phục ảnh {} từ thùng rác", row.publicId());
            return new RestoredMedia(row.publicId(), secureUrl);
        } catch (Exception exception) {
            // Lỗi của uploader Cloudinary là RuntimeException mang nguyên câu báo lỗi của API.
            String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
            if (message.contains("already exists")) {
                throw new MediaTrashException("MEDIA_TRASH_CONFLICT",
                        "Thư viện đã có ảnh khác trùng tên " + row.publicId() + " — xoá hoặc đổi tên ảnh đó trước", exception);
            }
            if (message.contains("not found")) {
                // File không còn trên Cloudinary (ai đó xoá thẳng trên trang quản trị Cloudinary) — dọn dòng mồ côi.
                jdbcTemplate.update("DELETE FROM media_trash WHERE trash_id = ?", trashId);
                throw new MediaTrashException("MEDIA_TRASH_GONE", "Ảnh không còn trên Cloudinary nên không khôi phục được", exception);
            }
            throw new MediaTrashException("CLOUDINARY_RESTORE_FAILED", "Cloudinary không khôi phục được ảnh", exception);
        }
    }

    /** Xoá hẳn khỏi Cloudinary. Không quay lại được. */
    public void purge(long trashId) {
        TrashRow row = requireRow(trashId);
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(row.publicId(), Map.of(
                    "resource_type", "image",
                    "type", TRASH_TYPE,
                    "invalidate", true));
            String status = Objects.toString(result.get("result"), "").toLowerCase();
            if (!"ok".equals(status) && !"not found".equals(status)) {
                throw new IOException("Cloudinary trả về kết quả xoá không mong đợi: " + status);
            }
            jdbcTemplate.update("DELETE FROM media_trash WHERE trash_id = ?", trashId);
            log.info("Đã xoá vĩnh viễn ảnh {} (Cloudinary: {})", row.publicId(), status);
        } catch (Exception exception) {
            throw new MediaTrashException("CLOUDINARY_DELETE_FAILED", "Cloudinary không xoá được ảnh", exception);
        }
    }

    private String previewUrl(String publicId, String format) {
        try {
            var url = cloudinary.url()
                    .resourceType("image")
                    .type(TRASH_TYPE)
                    .secure(true)
                    .signed(true)
                    .transformation(new Transformation<>().width(480).crop("limit"));
            if (format != null && !format.isBlank()) url = url.format(format);
            return url.generate(publicId);
        } catch (Exception exception) {
            return null;
        }
    }

    private TrashRow requireRow(long trashId) {
        List<TrashRow> rows = jdbcTemplate.query(
                "SELECT public_id, original_link FROM media_trash WHERE trash_id = ?",
                (rs, i) -> new TrashRow(rs.getString("public_id"), rs.getString("original_link")),
                trashId);
        if (rows.isEmpty()) {
            throw new DataTrashService.TrashNotFoundException("Ảnh không còn trong thùng rác (có thể đã được khôi phục)");
        }
        return rows.get(0);
    }

    private static Actor currentActor() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof StaffPrincipal principal) {
                String name = principal.getStaffAuth() != null && principal.getStaffAuth().getStaffProfile() != null
                        ? principal.getStaffAuth().getStaffProfile().getFullName()
                        : null;
                return new Actor(principal.getStaffId(), name);
            }
        } catch (Exception ignored) {
            // không có người dùng — vẫn cho vào thùng rác, chỉ thiếu tên người xoá
        }
        return new Actor(null, null);
    }

    private record Actor(Integer staffId, String name) {}

    private record TrashRow(String publicId, String originalLink) {}

    public record MediaTrashItem(
            long trashId,
            String publicId,
            String folder,
            String format,
            Integer width,
            Integer height,
            Long bytes,
            String deletedByName,
            String deletedAt,
            String previewUrl) {}

    public record MediaTrashPage(List<MediaTrashItem> items, long total, long totalBytes, int page, int size) {}

    public record RestoredMedia(String publicId, String secureUrl) {}

    public static class MediaTrashException extends RuntimeException {
        private final String code;

        public MediaTrashException(String code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
