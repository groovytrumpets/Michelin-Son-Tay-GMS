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
}
