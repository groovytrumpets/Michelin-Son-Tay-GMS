package com.g42.platform.gms.authz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Trả về cho FE ngay sau khi đăng nhập và mỗi lần nạp lại app. FE dựa vào đây để
 * ẩn/hiện menu và nút bấm — chốt chặn thật vẫn là @PreAuthorize ở backend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyPermissionsDto {
    private Integer staffId;
    private List<String> roles;
    private List<String> permissions;
    private boolean superRole;
}
