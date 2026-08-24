package com.g42.platform.gms.catalogdraft.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class CatalogDraftRequest {
    private String draftType;
    private String title;
    private JsonNode payload;
}
