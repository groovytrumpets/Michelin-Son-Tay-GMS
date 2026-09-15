package com.g42.platform.gms.staff.profile.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "role", schema = "michelin_garage")
public class RoleJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id", nullable = false)
    private Integer id;

    @Size(max = 50)
    @NotNull
    @Column(name = "role_code", nullable = false, length = 50)
    private String roleCode;

    @Size(max = 100)
    @NotNull
    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    /**
     * Vai trò gốc của hệ thống. Không cho xoá hay đổi role_code, vì ngoài việc
     * phân quyền thì mã này còn là dữ liệu nghiệp vụ: TicketAssignmentService,
     * CheckInService, TicketBackfillService tra "ai là KTV", "ai là cố vấn"
     * đúng bằng chuỗi "TECHNICIAN"/"ADVISOR".
     */
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = Boolean.FALSE;

    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;
}