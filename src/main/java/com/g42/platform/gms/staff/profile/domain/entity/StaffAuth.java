package com.g42.platform.gms.staff.profile.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaffAuth {
  private Integer staffAuthId;
  private String email;
  private String passwordHash;
  private String authProvider;
  private String status;
  private long failedLoginCount;
  private java.sql.Timestamp lockedUntil;
  private java.sql.Timestamp lastLoginAt;
  private java.sql.Timestamp createdAt;
  private String google_id;
  private Integer staffProfileId;

}
