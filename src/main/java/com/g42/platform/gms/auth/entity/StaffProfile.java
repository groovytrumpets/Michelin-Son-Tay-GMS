package com.g42.platform.gms.auth.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "staff_profile")
public class StaffProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer staffId;
    private String fullName;
    private String phone;
    private String position;
    private String gender;
    private java.sql.Date dob;
    private String avatar;
    private java.sql.Timestamp createdAt;

    @Column(name = "employee_no", unique = true)
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

    @OneToOne(mappedBy = "staffProfile")
    private StaffAuth staffauth;

    @OneToMany(mappedBy = "staff", fetch = FetchType.EAGER)
    private List<StaffRole> staffRoles = new ArrayList<>();

}
