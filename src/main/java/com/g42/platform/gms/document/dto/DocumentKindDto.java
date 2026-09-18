package com.g42.platform.gms.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dạng chứng từ. {@code templateCount} và {@code defaultTemplateName} chỉ để
 * hiển thị ở danh sách, gửi lên khi lưu cũng bị bỏ qua.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentKindDto {
    private Integer kindId;
    private String code;
    private String name;
    private String dataSource;
    private String stage;
    private String docNoPattern;
    private String description;
    private Boolean system;
    private Boolean active;
    private Integer sortOrder;
    private Integer templateCount;
    private String defaultTemplateName;
}
