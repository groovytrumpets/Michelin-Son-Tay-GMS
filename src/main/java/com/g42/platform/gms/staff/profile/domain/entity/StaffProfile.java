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
    private java.sql.Date startDate;
    private Boolean isResigned;
    private String permanentAddress;
    private String placeOfBirth;
    private String address;
    private String representative;
    private String ethnicity;
    private String religion;
    private String nationality;
    private String identityCard;
    private String idIssuePlace;
    private java.sql.Date idIssueDate;
    private String pitCode;
    private String pitIssuePlace;
    private java.sql.Date pitIssueDate;
    private String socialInsuranceCode;
    private String siIssuePlace;
    private java.sql.Date siIssueDate;
    private String siPaidPeriod;
    private String uiPaidPeriod;
    private String educationLevel;
    private String profession;
    private String department;
}
