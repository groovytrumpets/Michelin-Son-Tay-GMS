package com.g42.platform.gms.manager.employee.api.dto;

import lombok.Builder;
import lombok.Data;

import java.sql.Date;

@Data
@Builder
public class EmployeeResponse {
    private Integer staffId;
    private String fullName;
    private String phone;
    private String position;
    private String gender;
    private Date dob;
    private String avatar;
}
