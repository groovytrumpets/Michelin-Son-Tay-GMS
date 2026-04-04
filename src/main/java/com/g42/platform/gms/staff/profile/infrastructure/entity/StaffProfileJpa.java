package com.g42.platform.gms.staff.profile.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "staff_profile")
public class StaffProfileJpa {
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
    @OneToOne(mappedBy = "staffProfile", fetch = FetchType.LAZY)
    private StaffAuthJpa staffAuth;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "staff_role",
            joinColumns = @JoinColumn(name = "staff_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<RoleJpa> roles = new ArrayList<>();
}
