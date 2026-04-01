package com.g42.platform.gms.hikvision.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HikvisionEvent {
    private String employeeNoString;

    // Hikvision device returns "time" field (not "dateTime")
    @JsonProperty("time")
    private String dateTime;

    private String name;
    private Integer major;
    private Integer minor;
    private Integer cardType;
    private Integer cardReaderNo;
    private Integer doorNo;
    private String userType;
    private String currentVerifyMode;
}
