package com.g42.platform.gms.hikvision.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HikvisionEventResponse {

    private AcsEventCond AcsEventCond;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AcsEventCond {
        private List<HikvisionEvent> InfoList;
        private Integer total;
    }
}
