package com.g42.platform.gms.catalogdraft.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CatalogDraftResponse {
    private Long draftId;
    private String draftType;
    private String title;
    private JsonNode payload;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
