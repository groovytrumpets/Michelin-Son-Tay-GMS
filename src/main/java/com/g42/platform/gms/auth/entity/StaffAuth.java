package com.g42.platform.gms.auth.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "staff_auth")
public class StaffAuth {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)

  private long staffAuthId;
  private String email;
  private String passwordHash;
  private String authProvider;
  private String status;
  private long failedLoginCount;
  private java.sql.Timestamp lockedUntil;
  private java.sql.Timestamp lastLoginAt;
  private java.sql.Timestamp createdAt;

  @OneToOne
  @JoinColumn(name = "staff_id")
  private StaffProfile staffProfile;

}
