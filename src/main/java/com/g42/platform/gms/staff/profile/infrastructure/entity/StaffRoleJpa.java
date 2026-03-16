package com.g42.platform.gms.staff.profile.infrastructure.entity;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.entity.StaffRoleId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "staff_role", schema = "michelin_garage")
public class StaffRoleJpa {
    @EmbeddedId
    private StaffRoleId id;

    @MapsId("staffId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffProfile staff;


}