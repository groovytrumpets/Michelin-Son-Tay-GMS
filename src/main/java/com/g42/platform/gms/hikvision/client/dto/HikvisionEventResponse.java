package com.g42.platform.gms.hikvision.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HikvisionEventResponse {

    // Hikvision returns: {"AcsEvent": {"InfoList": [...], "totalMatches": N, ...}}
    @JsonProperty("AcsEvent")
    private AcsEvent acsEvent;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AcsEvent {
        @JsonProperty("InfoList")
        private List<HikvisionEvent> infoList;

        private Integer totalMatches;
        private Integer numOfMatches;
        private String responseStatusStrg;
        private String searchID;
    }
}
