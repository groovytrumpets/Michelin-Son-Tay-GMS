package com.g42.platform.gms.authz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDto {
    private String code;
    private String moduleCode;
    private String moduleLabel;
    private String groupLabel;
    private String actionCode;
    private String label;
    private String description;
    private Integer sortOrder;

    /** Vai trò (ngoài ADMIN) được phép giữ mã này; null = mọi vai trò. FE khoá ô tick của vai trò khác. */
    private java.util.List<String> allowedRoles;
}
