package com.g42.platform.gms.authz.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Một mã quyền trong danh mục. Bảng này chỉ đọc lúc chạy — thêm/bớt mã là việc
 * của changeset Liquibase, vì mỗi mã phải có code kiểm tra nó mới có tác dụng.
 */
@Getter
@Setter
@Entity
@Table(name = "permission")
public class PermissionJpa {

    @Id
    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "module_code", nullable = false, length = 48)
    private String moduleCode;

    @Column(name = "module_label", nullable = false, length = 100)
    private String moduleLabel;

    @Column(name = "group_label", nullable = false, length = 100)
    private String groupLabel;

    @Column(name = "action_code", nullable = false, length = 24)
    private String actionCode;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
