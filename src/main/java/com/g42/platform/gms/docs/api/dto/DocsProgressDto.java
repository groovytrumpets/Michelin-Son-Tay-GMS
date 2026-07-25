package com.g42.platform.gms.docs.api.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class DocsProgressDto {
    private Long id;
    private Integer staffId;
    private String topicId;
    private String sectionId;
    private String status;
    private Integer score;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime completedAt;
}
