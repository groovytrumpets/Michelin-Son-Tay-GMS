package com.g42.platform.gms.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bộ "In chứng từ" của một màn hình.
 *
 * {@code customized = false} nghĩa là màn đó chưa được chỉnh ở /document-template,
 * frontend tự dùng bộ mặc định trong code. {@code kinds} chỉ để hiển thị (tên,
 * khuôn số hiệu của các dạng có trong document_kind), gửi lên khi lưu bị bỏ qua.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentPrintSetDto {
    private String screenCode;
    private List<String> kindCodes;
    private String defaultKindCode;
    private Boolean customized;
    private LocalDateTime updatedAt;
    private List<DocumentKindDto> kinds;
}
