package com.g42.platform.gms.datasafety.api.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.datasafety.application.service.DataTrashService;
import com.g42.platform.gms.datasafety.application.service.DataTrashService.RestoreResult;
import com.g42.platform.gms.datasafety.application.service.DataTrashService.TrashConflictException;
import com.g42.platform.gms.datasafety.application.service.DataTrashService.TrashNotFoundException;
import com.g42.platform.gms.datasafety.application.service.DataTrashService.TrashPage;
import com.g42.platform.gms.datasafety.application.service.DataTrashService.TrashTypeSummary;
import com.g42.platform.gms.datasafety.application.service.MediaTrashService;
import com.g42.platform.gms.datasafety.application.service.MediaTrashService.MediaTrashException;
import com.g42.platform.gms.datasafety.application.service.MediaTrashService.MediaTrashPage;
import com.g42.platform.gms.datasafety.application.service.MediaTrashService.RestoredMedia;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Màn /data-backup, phần thùng rác: dữ liệu xoá mềm của các module + ảnh đã xoá
 * khỏi thư viện ảnh. Chỉ ADMIN và MANAGER (mã quyền khoá theo vai trò).
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/data-trash")
public class DataTrashController {

    private final DataTrashService dataTrashService;
    private final MediaTrashService mediaTrashService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DATA_SAFETY_VIEW + "')")
    public ResponseEntity<ApiResponse<TrashOverview>> summary() {
        Long mediaCount;
        try {
            mediaCount = mediaTrashService.count();
        } catch (Exception exception) {
            // Bảng media_trash chưa có (chưa restart BE sau Liquibase 050) — phần dữ liệu vẫn xem được.
            log.warn("Không đếm được thùng rác ảnh: {}", exception.getMessage());
            mediaCount = null;
        }
        return ResponseEntity.ok(ApiResponses.success(new TrashOverview(dataTrashService.summary(), mediaCount)));
    }

    @GetMapping("/items")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DATA_SAFETY_VIEW + "')")
    public ResponseEntity<ApiResponse<TrashPage>> list(
            @RequestParam String type,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponses.success(dataTrashService.list(type, search, page, size)));
    }

    @PostMapping("/items/{type}/{id}/restore")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DATA_TRASH_RESTORE + "')")
    @Auditable(action = "UPDATE", module = "DATA_TRASH", severity = "WARNING",
            description = "Khôi phục dữ liệu đã xoá từ thùng rác", targetType = "TRASH_ITEM")
    public ResponseEntity<ApiResponse<RestoreResult>> restore(@PathVariable String type, @PathVariable long id) {
        RestoreResult result = dataTrashService.restore(type, id);
        return ResponseEntity.ok(ApiResponses.success(result, "Đã khôi phục " + result.label().toLowerCase()
                + (result.title() == null ? "" : " \"" + result.title() + "\"")));
    }

    @GetMapping("/media")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DATA_SAFETY_VIEW + "')")
    public ResponseEntity<ApiResponse<MediaTrashPage>> listMedia(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return ResponseEntity.ok(ApiResponses.success(mediaTrashService.list(search, page, size)));
    }

    @PostMapping("/media/{trashId}/restore")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DATA_TRASH_RESTORE + "')")
    @Auditable(action = "UPDATE", module = "DATA_TRASH", severity = "WARNING",
            description = "Khôi phục ảnh từ thùng rác", targetType = "MEDIA")
    public ResponseEntity<ApiResponse<RestoredMedia>> restoreMedia(@PathVariable long trashId) {
        return ResponseEntity.ok(ApiResponses.success(mediaTrashService.restore(trashId), "Đã khôi phục ảnh về thư viện"));
    }

    @DeleteMapping("/media/{trashId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DATA_TRASH_PURGE + "')")
    @Auditable(action = "DELETE", module = "DATA_TRASH", severity = "CRITICAL",
            description = "Xoá vĩnh viễn ảnh trong thùng rác", targetType = "MEDIA")
    public ResponseEntity<ApiResponse<Void>> purgeMedia(@PathVariable long trashId) {
        mediaTrashService.purge(trashId);
        return ResponseEntity.ok(ApiResponses.successMessage("Đã xoá vĩnh viễn ảnh"));
    }

    // ------------------------------------------------------------ lỗi

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(ApiResponses.error("INVALID_TRASH_REQUEST", exception.getMessage()));
    }

    @ExceptionHandler(TrashNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(TrashNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponses.error("TRASH_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(TrashConflictException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(TrashConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponses.error("TRASH_CONFLICT", exception.getMessage()));
    }

    @ExceptionHandler(MediaTrashException.class)
    public ResponseEntity<ApiResponse<Void>> mediaFailure(MediaTrashException exception) {
        HttpStatus status = switch (exception.getCode()) {
            case "MEDIA_TRASH_CONFLICT" -> HttpStatus.CONFLICT;
            case "MEDIA_TRASH_GONE" -> HttpStatus.GONE;
            // Cloudinary lỗi: cố ý không trả 5xx — FE (ServerDownOverlay) coi mọi 5xx là "mất kết nối
            // máy chủ" và che cả màn hình, trong khi backend vẫn sống, chỉ dịch vụ ảnh bên ngoài lỗi.
            default -> HttpStatus.FAILED_DEPENDENCY;
        };
        if (status == HttpStatus.FAILED_DEPENDENCY) {
            log.error("Thùng rác ảnh: thao tác Cloudinary lỗi ({})", exception.getCode(), exception);
        }
        return ResponseEntity.status(status).body(ApiResponses.error(exception.getCode(), exception.getMessage()));
    }

    public record TrashOverview(List<TrashTypeSummary> types, Long mediaCount) {}
}
