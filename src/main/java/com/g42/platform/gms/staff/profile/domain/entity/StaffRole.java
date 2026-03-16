package com.g42.platform.gms.staff.profile.domain.entity;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.entity.StaffRoleId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaffRole {
    private StaffRoleId id;
    private StaffProfile staff;
    private Integer roleId;
}