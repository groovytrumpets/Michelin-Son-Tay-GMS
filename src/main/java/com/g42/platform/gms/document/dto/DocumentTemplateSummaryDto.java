package com.g42.platform.gms.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Dòng trong danh sách mẫu — cố tình không kèm layoutJson cho nhẹ. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTemplateSummaryDto {
    private Integer templateId;
    private Integer kindId;
    private String name;
    private String paperSize;
    private String note;
    private Boolean defaultTemplate;
    private Boolean active;
    private Integer version;
    private Integer blockCount;
    private LocalDateTime updatedAt;
}
