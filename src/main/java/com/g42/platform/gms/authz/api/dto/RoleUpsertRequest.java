package com.g42.platform.gms.authz.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpsertRequest {
    /** Chỉ dùng khi tạo mới; vai trò đã có thì không cho đổi mã. */
    private String roleCode;
    private String roleName;
    private String description;
}
