package com.g42.platform.gms.docs.api.dto;

import lombok.Data;

@Data
public class DocsProgressUpdateDto {
    private Integer staffId;
    private String topicId;
    private String sectionId;
    private String status; // IN_PROGRESS, COMPLETED
    private Integer score;
}
