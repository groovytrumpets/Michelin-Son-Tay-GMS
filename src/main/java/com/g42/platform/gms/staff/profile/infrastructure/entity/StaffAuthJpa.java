package com.g42.platform.gms.staff.profile.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "staff_auth")
public class StaffAuthJpa {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)

  private Integer staffAuthId;
  private String email;
  private String passwordHash;
  private String authProvider;
  private String status;
  private long failedLoginCount;
  private java.sql.Timestamp lockedUntil;
  private java.sql.Timestamp lastLoginAt;
  private java.sql.Timestamp createdAt;
  @Column(name = "google_id")
  private String googleId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "staff_id")
  private StaffProfileJpa staffProfile;

}
