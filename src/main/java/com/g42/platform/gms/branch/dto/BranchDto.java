package com.g42.platform.gms.branch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchDto {
    private Integer branchId;
    private String branchCode;
    private String branchName;
    private String address;
    private String phone;
    private Boolean isDefault;
    private Boolean isActive;
    private Integer sortOrder;
    /** Chỉ có ở danh sách quản trị: số phiếu đã ghi xưởng này (để biết xoá được hay chỉ ẩn được). */
    private Long ticketCount;
}
