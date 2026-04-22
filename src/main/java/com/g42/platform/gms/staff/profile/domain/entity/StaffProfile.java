package com.g42.platform.gms.staff.profile.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaffProfile {
    private Integer staffId;
    private String fullName;
    private String email;
    private String status;
    private String phone;
    private String position;
    private String gender;
    private java.sql.Date dob;
    private String avatar;
    private java.sql.Timestamp createdAt;
    private List<Role> roles;
    private String employeeNo;
}
