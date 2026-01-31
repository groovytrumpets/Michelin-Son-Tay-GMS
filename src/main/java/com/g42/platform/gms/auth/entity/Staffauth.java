package com.g42.platform.gms.auth.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "staff_auth")
public class Staffauth {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)

  private long staffAuthId;
  private long staffId;
  private String email;
  private String passwordHash;
  private String authProvider;
  private String status;
  private long failedLoginCount;
  private java.sql.Timestamp lockedUntil;
  private java.sql.Timestamp lastLoginAt;
  private java.sql.Timestamp createdAt;


  public long getStaffAuthId() {
    return staffAuthId;
  }

  public void setStaffAuthId(long staffAuthId) {
    this.staffAuthId = staffAuthId;
  }


  public long getStaffId() {
    return staffId;
  }

  public void setStaffId(long staffId) {
    this.staffId = staffId;
  }


  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }


  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }


  public String getAuthProvider() {
    return authProvider;
  }

  public void setAuthProvider(String authProvider) {
    this.authProvider = authProvider;
  }


  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }


  public long getFailedLoginCount() {
    return failedLoginCount;
  }

  public void setFailedLoginCount(long failedLoginCount) {
    this.failedLoginCount = failedLoginCount;
  }


  public java.sql.Timestamp getLockedUntil() {
    return lockedUntil;
  }

  public void setLockedUntil(java.sql.Timestamp lockedUntil) {
    this.lockedUntil = lockedUntil;
  }


  public java.sql.Timestamp getLastLoginAt() {
    return lastLoginAt;
  }

  public void setLastLoginAt(java.sql.Timestamp lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }


  public java.sql.Timestamp getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(java.sql.Timestamp createdAt) {
    this.createdAt = createdAt;
  }

}
