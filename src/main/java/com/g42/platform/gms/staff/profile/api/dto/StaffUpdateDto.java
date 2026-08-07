package com.g42.platform.gms.staff.profile.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaffUpdateDto {
    private String fullName;
    private String phone;
    private String position;
    private String avatar;
    private String status;
    private java.sql.Date dob;
    private List<RoleDto> roles;
    private String gender;
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
