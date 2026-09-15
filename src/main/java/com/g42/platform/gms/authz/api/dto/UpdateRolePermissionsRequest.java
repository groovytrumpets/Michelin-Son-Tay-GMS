package com.g42.platform.gms.authz.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateRolePermissionsRequest {
    /** Bộ quyền đầy đủ sau khi sửa — gửi cả danh sách, không gửi phần chênh lệch. */
    private List<String> permissionCodes;
}
