package com.g42.platform.gms.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mẫu chứng từ kèm bố cục đầy đủ.
 *
 * {@code layoutJson} có thể lên tới vài trăm KB khi mẫu dùng nền PDF, nên danh
 * sách mẫu trả về {@link DocumentTemplateSummaryDto} không kèm trường này; chỉ
 * khi mở đúng một mẫu ra sửa mới tải cả bố cục.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTemplateDto {
    private Integer templateId;
    private Integer kindId;
    private String kindCode;
    private String kindName;
    private String dataSource;
    private String stage;
    private String name;
    private String layoutJson;
    private String sourceFileName;
    private String paperSize;
    private String note;
    private Boolean defaultTemplate;
    private Boolean active;
    private Integer version;
}
