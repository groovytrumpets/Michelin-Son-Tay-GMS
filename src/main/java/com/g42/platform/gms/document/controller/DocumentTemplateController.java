package com.g42.platform.gms.document.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.document.dto.CompanyProfileDto;
import com.g42.platform.gms.document.dto.DocumentKindDto;
import com.g42.platform.gms.document.dto.DocumentTemplateDto;
import com.g42.platform.gms.document.dto.DocumentTemplateSummaryDto;
import com.g42.platform.gms.document.service.DocumentTemplateService;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Biểu mẫu chứng từ — dạng chứng từ, bố cục mẫu, hồ sơ công ty.
 *
 * Phần ĐỌC mở cho mọi nhân viên có DOCUMENT_TEMPLATE_VIEW: các màn bán hàng,
 * sửa xe, kho đều phải đọc được mẫu thì mới in ra giấy được. Phần SỬA bị bó vào
 * DOCUMENT_TEMPLATE_EDIT vì đây là chứng từ pháp lý.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/document-templates")
@RequiredArgsConstructor
public class DocumentTemplateController {

    /** Ai được đọc mẫu + hồ sơ công ty để IN — người quản lý biểu mẫu và các màn nghiệp vụ có nút in. */
    private static final String PRINT_READERS = "hasAnyAuthority('"
            + PermissionCodes.DOCUMENT_TEMPLATE_VIEW + "','"
            + PermissionCodes.PARTS_SALE_VIEW + "','"
            + PermissionCodes.PARTS_SALE_CREATE + "')";

    private final DocumentTemplateService documentTemplateService;

    // ---------------------------------------------------------------- dạng chứng từ

    @GetMapping("/kinds")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DOCUMENT_TEMPLATE_VIEW + "')")
    public ResponseEntity<ApiResponse<List<DocumentKindDto>>> listKinds(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponses.success(documentTemplateService.listKinds(activeOnly)));
    }

    /**
     * Dạng chứng từ in được tại một điểm trong luồng nghiệp vụ. Màn tiếp nhận xe
     * gọi {@code ?dataSource=SERVICE_TICKET&stage=INTAKE}, màn thu tiền gọi
     * {@code stage=PAYMENT}. Bỏ stage thì lấy hết của nguồn đó.
     */
    @GetMapping("/kinds/available")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DOCUMENT_TEMPLATE_VIEW + "')")
    public ResponseEntity<ApiResponse<List<DocumentKindDto>>> listAvailableKinds(
            @RequestParam String dataSource,
            @RequestParam(required = false) String stage) {
        try {
            return ResponseEntity.ok(ApiResponses.success(
                    documentTemplateService.listKindsFor(dataSource, stage)));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        }
    }

    @PostMapping("/kinds")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DOCUMENT_TEMPLATE_EDIT + "')")
    @Auditable(action = "CREATE", module = "DOCUMENT_TEMPLATE",
            description = "Thêm dạng chứng từ", targetType = "DOCUMENT_KIND")
    public ResponseEntity<ApiResponse<DocumentKindDto>> createKind(@RequestBody DocumentKindDto dto) {
        try {
            return ResponseEntity.ok(ApiResponses.success(documentTemplateService.createKind(dto)));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        }
    }

    @PutMapping("/kinds/{kindId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DOCUMENT_TEMPLATE_EDIT + "')")
    @Auditable(action = "UPDATE", module = "DOCUMENT_TEMPLATE",
            description = "Sửa dạng chứng từ", targetType = "DOCUMENT_KIND")
    public ResponseEntity<ApiResponse<DocumentKindDto>> updateKind(@PathVariable Integer kindId,
                                                                   @RequestBody DocumentKindDto dto) {
        try {
            return ResponseEntity.ok(ApiResponses.success(documentTemplateService.updateKind(kindId, dto)));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        }
    }

    @DeleteMapping("/kinds/{kindId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DOCUMENT_TEMPLATE_DELETE + "')")
    @Auditable(action = "DELETE", module = "DOCUMENT_TEMPLATE", severity = "WARNING",
            description = "Xoá dạng chứng từ", targetType = "DOCUMENT_KIND")
    public ResponseEntity<ApiResponse<Void>> deleteKind(@PathVariable Integer kindId) {
        try {
            documentTemplateService.deleteKind(kindId);
            return ResponseEntity.ok(ApiResponses.successMessage("Đã xoá dạng chứng từ"));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        }
    }

    // ---------------------------------------------------------------- mẫu

    @GetMapping("/kinds/{kindId}/templates")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DOCUMENT_TEMPLATE_VIEW + "')")
    public ResponseEntity<ApiResponse<List<DocumentTemplateSummaryDto>>> listTemplates(
            @PathVariable Integer kindId) {
        return ResponseEntity.ok(ApiResponses.success(documentTemplateService.listTemplates(kindId)));
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DOCUMENT_TEMPLATE_VIEW + "')")
    public ResponseEntity<ApiResponse<DocumentTemplateDto>> getTemplate(@PathVariable Integer templateId) {
        try {
            return ResponseEntity.ok(ApiResponses.success(documentTemplateService.getTemplate(templateId)));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        }
    }

    /**
     * Mẫu đang dùng của một dạng — các màn nghiệp vụ gọi cái này để in, nên mở cho
     * cả người có quyền ở màn đó chứ không chỉ người quản lý biểu mẫu. Thêm màn
     * in mới thì thêm mã quyền của màn đó vào {@link #PRINT_READERS}.
     */
    @GetMapping("/by-kind/{kindCode}/default")
    @PreAuthorize(PRINT_READERS)
    public ResponseEntity<ApiResponse<DocumentTemplateDto>> getDefaultTemplate(@PathVariable String kindCode) {
        try {
            return ResponseEntity.ok(ApiResponses.success(documentTemplateService.getDefaultTemplate(kindCode)));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.DOCUMENT_TEMPLATE_EDIT + "')")
    @Auditable(action = "CREATE", module = "DOCUMENT_TEMPLATE",
            description = "Thêm mẫu chứng từ", targetType = "DOCUMENT_TEMPLATE")
    public ResponseEntity<ApiResponse<DocumentTemplateDto>> create(
            @AuthenticationPrincipal StaffPrincipal principal,
            @RequestBody DocumentTemplateDto dto) {
        try {
            return ResponseEntity.ok(ApiResponses.success(
                    documentTemplateService.saveTemplate(null, dto, staffId(principal))));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        }
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DOCUMENT_TEMPLATE_EDIT + "')")
    @Auditable(action = "UPDATE", module = "DOCUMENT_TEMPLATE",
            description = "Lưu bố cục mẫu chứng từ", targetType = "DOCUMENT_TEMPLATE")
    public ResponseEntity<ApiResponse<DocumentTemplateDto>> update(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable Integer templateId,
            @RequestBody DocumentTemplateDto dto) {
        try {
            return ResponseEntity.ok(ApiResponses.success(
                    documentTemplateService.saveTemplate(templateId, dto, staffId(principal))));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        }
    }

    /**
     * Cài hoặc khôi phục mẫu gốc của một dạng — bố cục do frontend gửi lên từ bộ
     * mẫu dựng sẵn theo file Word. Mẫu gốc không xoá được.
     */
    @PostMapping("/kinds/{kindId}/system-template")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DOCUMENT_TEMPLATE_EDIT + "')")
    @Auditable(action = "UPDATE", module = "DOCUMENT_TEMPLATE",
            description = "Cài / khôi phục mẫu gốc chứng từ", targetType = "DOCUMENT_TEMPLATE")
    public ResponseEntity<ApiResponse<DocumentTemplateDto>> installSystemTemplate(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable Integer kindId,
            @RequestBody DocumentTemplateDto dto) {
        try {
            return ResponseEntity.ok(ApiResponses.success(
                    documentTemplateService.installSystemTemplate(kindId, dto, staffId(principal))));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        }
    }

    @PostMapping("/{templateId}/duplicate")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DOCUMENT_TEMPLATE_EDIT + "')")
    @Auditable(action = "CREATE", module = "DOCUMENT_TEMPLATE",
            description = "Nhân bản mẫu chứng từ", targetType = "DOCUMENT_TEMPLATE")
    public ResponseEntity<ApiResponse<DocumentTemplateDto>> duplicate(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable Integer templateId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String newName = body == null ? null : body.get("name");
            return ResponseEntity.ok(ApiResponses.success(
                    documentTemplateService.duplicateTemplate(templateId, newName, staffId(principal))));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        }
    }

    @PostMapping("/{templateId}/default")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DOCUMENT_TEMPLATE_EDIT + "')")
    @Auditable(action = "UPDATE", module = "DOCUMENT_TEMPLATE",
            description = "Đặt mẫu chứng từ mặc định", targetType = "DOCUMENT_TEMPLATE")
    public ResponseEntity<ApiResponse<Void>> setDefault(@PathVariable Integer templateId) {
        try {
            documentTemplateService.setDefaultTemplate(templateId);
            return ResponseEntity.ok(ApiResponses.successMessage("Đã đặt làm mẫu mặc định"));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        }
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DOCUMENT_TEMPLATE_DELETE + "')")
    @Auditable(action = "DELETE", module = "DOCUMENT_TEMPLATE", severity = "WARNING",
            description = "Xoá mẫu chứng từ", targetType = "DOCUMENT_TEMPLATE")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer templateId) {
        try {
            documentTemplateService.deleteTemplate(templateId);
            return ResponseEntity.ok(ApiResponses.successMessage("Đã xoá mẫu chứng từ"));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        }
    }

    // ---------------------------------------------------------------- hồ sơ công ty

    @GetMapping("/company-profile")
    @PreAuthorize(PRINT_READERS)
    public ResponseEntity<ApiResponse<CompanyProfileDto>> getCompanyProfile() {
        return ResponseEntity.ok(ApiResponses.success(documentTemplateService.getCompanyProfile()));
    }

    @PutMapping("/company-profile")
    @PreAuthorize("hasAuthority('" + PermissionCodes.DOCUMENT_TEMPLATE_EDIT + "')")
    @Auditable(action = "UPDATE", module = "DOCUMENT_TEMPLATE",
            description = "Sửa hồ sơ công ty in trên chứng từ", targetType = "COMPANY_PROFILE")
    public ResponseEntity<ApiResponse<CompanyProfileDto>> saveCompanyProfile(
            @RequestBody CompanyProfileDto dto) {
        try {
            return ResponseEntity.ok(ApiResponses.success(documentTemplateService.saveCompanyProfile(dto)));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        }
    }

    // ---------------------------------------------------------------- nội bộ

    private static Integer staffId(StaffPrincipal principal) {
        return principal == null ? null : principal.getStaffId();
    }

    private static <T> ResponseEntity<ApiResponse<T>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error("INVALID_DOCUMENT_TEMPLATE", e.getMessage()));
    }
}
