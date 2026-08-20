package com.g42.platform.gms.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.exceptions.NotFound;
import com.g42.platform.gms.common.constant.FileUploadConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Server-side gateway for the Cloudinary media library.
 * Cloudinary Admin API credentials must never be exposed to the browser.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaLibraryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_UPLOAD_FILES = 20;
    private static final String DEFAULT_UPLOAD_FOLDER = "garage/media-library";
    private static final Pattern SAFE_FOLDER = Pattern.compile("[A-Za-z0-9/_-]+");
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9_$]+", Pattern.UNICODE_CASE);
    private static final long COLUMN_CACHE_MILLIS = Duration.ofMinutes(10).toMillis();

    private static final String SEARCHABLE_COLUMNS_SQL = """
            SELECT TABLE_NAME, COLUMN_NAME
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND DATA_TYPE IN ('char', 'varchar', 'tinytext', 'text', 'mediumtext', 'longtext')
              AND (
                    LOWER(COLUMN_NAME) LIKE '%url%'
                 OR LOWER(COLUMN_NAME) LIKE '%image%'
                 OR LOWER(COLUMN_NAME) LIKE '%photo%'
                 OR LOWER(COLUMN_NAME) LIKE '%avatar%'
                 OR LOWER(COLUMN_NAME) LIKE '%thumbnail%'
                 OR LOWER(COLUMN_NAME) LIKE '%content%'
              )
            ORDER BY TABLE_NAME, ORDINAL_POSITION
            """;

    private final Cloudinary cloudinary;
    private final ImageUploadService imageUploadService;
    private final JdbcTemplate jdbcTemplate;

    private volatile List<ColumnReference> cachedColumns = List.of();
    private volatile long cachedColumnsAt;

    public MediaPage listImages(String cursor, int requestedSize, String prefix) {
        int maxResults = Math.min(Math.max(requestedSize, 1), MAX_PAGE_SIZE);
        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "image");
        options.put("type", "upload");
        options.put("max_results", maxResults);
        if (hasText(cursor)) options.put("next_cursor", cursor.trim());
        if (hasText(prefix)) options.put("prefix", prefix.trim());

        try {
            Map<?, ?> response = cloudinary.api().resources(options);
            List<MediaAsset> resources = new ArrayList<>();
            Object rawResources = response.get("resources");
            if (rawResources instanceof List<?> list) {
                for (Object value : list) {
                    if (value instanceof Map<?, ?> resource) {
                        MediaAsset asset = toAsset(resource);
                        if (hasText(asset.publicId()) && hasText(asset.secureUrl())) resources.add(asset);
                    }
                }
            }
            return new MediaPage(resources, stringValue(response.get("next_cursor")), numberValue(response.get("total_count")));
        } catch (Exception exception) {
            throw new MediaLibraryException("CLOUDINARY_LIST_FAILED", "Không thể lấy danh sách ảnh từ Cloudinary", exception);
        }
    }

    public List<UploadedAsset> uploadImages(List<MultipartFile> files, String requestedFolder) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một ảnh");
        }
        if (files.size() > MAX_UPLOAD_FILES) {
            throw new IllegalArgumentException("Mỗi lần chỉ được tải tối đa " + MAX_UPLOAD_FILES + " ảnh");
        }

        String folder = normalizeFolder(requestedFolder);
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException(FileUploadConstants.ERROR_FILE_EMPTY);
            }
            if (file.getSize() > FileUploadConstants.MAX_IMAGE_SIZE_BYTES) {
                throw new IllegalArgumentException(
                        String.format(FileUploadConstants.ERROR_FILE_TOO_LARGE, FileUploadConstants.MAX_IMAGE_SIZE_MB));
            }
        }
        List<UploadedAsset> uploaded = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                String url = imageUploadService.uploadImage(file, folder);
                uploaded.add(new UploadedAsset(imageUploadService.extractPublicId(url), url));
            }
            return uploaded;
        } catch (IllegalArgumentException exception) {
            rollbackUploadedAssets(uploaded);
            throw exception;
        } catch (IOException exception) {
            rollbackUploadedAssets(uploaded);
            throw new MediaLibraryException("CLOUDINARY_UPLOAD_FAILED", "Không thể tải ảnh lên Cloudinary", exception);
        }
    }

    public MediaUsage inspectUsage(String publicId) {
        String normalizedPublicId = normalizePublicId(publicId);
        MediaAsset asset = getImage(normalizedPublicId);
        return scanReferences(normalizedPublicId, asset.secureUrl());
    }

    /** Re-check references immediately before destroy to keep deletion fail-safe. */
    public DeleteResult deleteImage(String publicId) {
        String normalizedPublicId = normalizePublicId(publicId);
        MediaUsage usage = inspectUsage(normalizedPublicId);
        if (!"SAFE".equals(usage.referenceStatus())) {
            String message = "IN_USE".equals(usage.referenceStatus())
                    ? "Ảnh đang được sử dụng tại " + usage.referenceCount() + " vị trí"
                    : "Chưa thể xác minh đầy đủ nơi đang sử dụng ảnh";
            throw new MediaInUseException(message, usage);
        }

        try {
            String result = imageUploadService.deleteImageWithResult(normalizedPublicId);
            log.info("Media library image deleted: publicId={}, cloudinaryResult={}", normalizedPublicId, result);
            return new DeleteResult(normalizedPublicId, result, true);
        } catch (IOException exception) {
            throw new MediaLibraryException("CLOUDINARY_DELETE_FAILED", "Cloudinary không thể xóa ảnh", exception);
        }
    }

    private MediaAsset getImage(String publicId) {
        Map<String, Object> options = Map.of("resource_type", "image", "type", "upload");
        try {
            Map<?, ?> response = cloudinary.api().resource(publicId, options);
            MediaAsset asset = toAsset(response);
            if (!hasText(asset.secureUrl())) {
                throw new MediaNotFoundException("Không tìm thấy ảnh trên Cloudinary");
            }
            return asset;
        } catch (MediaNotFoundException exception) {
            throw exception;
        } catch (NotFound exception) {
            throw new MediaNotFoundException("Không tìm thấy ảnh trên Cloudinary");
        } catch (Exception exception) {
            throw new MediaLibraryException("CLOUDINARY_RESOURCE_FAILED", "Không thể đọc thông tin ảnh từ Cloudinary", exception);
        }
    }

    private MediaUsage scanReferences(String publicId, String secureUrl) {
        List<ColumnReference> columns;
        try {
            columns = searchableColumns();
        } catch (DataAccessException exception) {
            log.error("Cannot read database metadata while checking media references", exception);
            return new MediaUsage(publicId, null, "UNKNOWN", List.of());
        }

        boolean complete = true;
        long total = 0;
        List<MediaReference> references = new ArrayList<>();
        for (ColumnReference column : columns) {
            if (!isSafeIdentifier(column.table()) || !isSafeIdentifier(column.column())) {
                complete = false;
                continue;
            }
            String sql = "SELECT COUNT(*) FROM `" + column.table() + "` WHERE `" + column.column()
                    + "` LIKE CONCAT('%', ?, '%') OR `" + column.column() + "` LIKE CONCAT('%', ?, '%')";
            try {
                Long count = jdbcTemplate.queryForObject(sql, Long.class, secureUrl, publicId);
                long matches = count == null ? 0 : count;
                if (matches > 0) {
                    total += matches;
                    references.add(new MediaReference(column.table(), column.column(), matches));
                }
            } catch (DataAccessException exception) {
                complete = false;
                log.warn("Cannot check media reference in {}.{}: {}",
                        column.table(), column.column(), exception.getMostSpecificCause().getMessage());
            }
        }

        if (total > 0) return new MediaUsage(publicId, total, "IN_USE", references);
        if (!complete) return new MediaUsage(publicId, null, "UNKNOWN", references);
        return new MediaUsage(publicId, 0L, "SAFE", references);
    }

    private List<ColumnReference> searchableColumns() {
        long now = System.currentTimeMillis();
        List<ColumnReference> current = cachedColumns;
        if (!current.isEmpty() && now - cachedColumnsAt < COLUMN_CACHE_MILLIS) return current;

        synchronized (this) {
            if (!cachedColumns.isEmpty() && now - cachedColumnsAt < COLUMN_CACHE_MILLIS) return cachedColumns;
            List<ColumnReference> loaded = jdbcTemplate.query(
                    SEARCHABLE_COLUMNS_SQL,
                    (resultSet, rowNumber) -> new ColumnReference(
                            resultSet.getString("TABLE_NAME"),
                            resultSet.getString("COLUMN_NAME")));
            cachedColumns = List.copyOf(loaded);
            cachedColumnsAt = now;
            return cachedColumns;
        }
    }

    private void rollbackUploadedAssets(List<UploadedAsset> uploaded) {
        for (UploadedAsset asset : uploaded) {
            try {
                imageUploadService.deleteImage(asset.publicId());
            } catch (Exception rollbackError) {
                log.warn("Cannot rollback partially uploaded image {}", asset.publicId(), rollbackError);
            }
        }
    }

    private String normalizeFolder(String requestedFolder) {
        String folder = hasText(requestedFolder) ? requestedFolder.trim() : DEFAULT_UPLOAD_FOLDER;
        folder = folder.replace('\\', '/').replaceAll("^/+|/+$", "");
        if (folder.length() > 120 || folder.contains("..") || !SAFE_FOLDER.matcher(folder).matches()
                || !(folder.equals("garage") || folder.startsWith("garage/"))) {
            throw new IllegalArgumentException("Thư mục Cloudinary không hợp lệ");
        }
        return folder;
    }

    private String normalizePublicId(String publicId) {
        if (!hasText(publicId)) throw new IllegalArgumentException("Thiếu public_id của ảnh");
        String normalized = publicId.trim();
        if (normalized.length() > 255 || normalized.contains("..")) {
            throw new IllegalArgumentException("public_id không hợp lệ");
        }
        return normalized;
    }

    private MediaAsset toAsset(Map<?, ?> resource) {
        String publicId = stringValue(resource.get("public_id"));
        String folder = firstText(stringValue(resource.get("asset_folder")), stringValue(resource.get("folder")));
        if (!hasText(folder) && hasText(publicId) && publicId.contains("/")) {
            folder = publicId.substring(0, publicId.lastIndexOf('/'));
        }
        if (!hasText(folder)) folder = "Không phân loại";

        return new MediaAsset(
                publicId,
                firstText(stringValue(resource.get("secure_url")), stringValue(resource.get("url"))),
                folder,
                stringValue(resource.get("format")),
                integerValue(resource.get("width")),
                integerValue(resource.get("height")),
                numberValue(resource.get("bytes")),
                stringValue(resource.get("created_at")),
                "image",
                null,
                "UNKNOWN",
                List.of());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String firstText(String first, String second) {
        return hasText(first) ? first : second;
    }

    private static String stringValue(Object value) {
        return value == null ? null : Objects.toString(value, null);
    }

    private static Long numberValue(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static Integer integerValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private static boolean isSafeIdentifier(String value) {
        return value != null && SAFE_IDENTIFIER.matcher(value).matches();
    }

    public record MediaPage(List<MediaAsset> resources, String nextCursor, Long totalCount) {}

    public record MediaAsset(
            String publicId,
            String secureUrl,
            String folder,
            String format,
            Integer width,
            Integer height,
            Long bytes,
            String createdAt,
            String resourceType,
            Long referenceCount,
            String referenceStatus,
            List<MediaReference> references) {}

    public record UploadedAsset(String publicId, String secureUrl) {}

    public record MediaReference(String source, String field, long count) {}

    public record MediaUsage(String publicId, Long referenceCount, String referenceStatus, List<MediaReference> references) {}

    public record DeleteResult(String publicId, String result, boolean invalidated) {}

    private record ColumnReference(String table, String column) {}

    public static class MediaLibraryException extends RuntimeException {
        private final String code;

        public MediaLibraryException(String code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public static class MediaNotFoundException extends RuntimeException {
        public MediaNotFoundException(String message) {
            super(message);
        }
    }

    public static class MediaInUseException extends RuntimeException {
        private final MediaUsage usage;

        public MediaInUseException(String message, MediaUsage usage) {
            super(message);
            this.usage = usage;
        }

        public MediaUsage getUsage() {
            return usage;
        }
    }
}
