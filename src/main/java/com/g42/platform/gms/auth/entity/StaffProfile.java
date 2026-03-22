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

    @OneToOne(mappedBy = "staffProfile")
    private StaffAuth staffauth;

    @OneToMany(mappedBy = "staff", fetch = FetchType.EAGER)
    private List<StaffRole> staffRoles = new ArrayList<>();

}
