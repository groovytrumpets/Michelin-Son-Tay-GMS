package com.g42.platform.gms.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class StaffRoleId implements Serializable {
    private static final long serialVersionUID = -696442086377832088L;
    @NotNull
    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @NotNull
    @Column(name = "role_id", nullable = false)
    private Integer roleId;


}