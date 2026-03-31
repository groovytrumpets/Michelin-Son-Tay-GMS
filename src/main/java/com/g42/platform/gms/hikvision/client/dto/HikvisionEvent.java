package com.g42.platform.gms.hikvision.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HikvisionEvent {
    private String employeeNoString;
    private String dateTime; // ISO 8601: "2024-01-15T08:30:00+07:00"
    private String name;
    private Integer major;
    private Integer minor;
}
