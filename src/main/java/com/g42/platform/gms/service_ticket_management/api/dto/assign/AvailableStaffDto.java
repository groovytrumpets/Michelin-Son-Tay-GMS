package com.g42.platform.gms.service_ticket_management.api.dto.assign;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvailableStaffDto {
    private Integer staffId;
    private String fullName;
    private String phone;
    private String avatar;
    private List<RoleDto> roles;

    // Availability info — hiển thị để nhân viên biết, không chặn assign
    private Boolean isBusy;           // true nếu đang có assignment ACTIVE/PENDING
    private String busyNote;          // Mô tả đang làm gì (vd: "Đang làm dịch vụ khác")
}
