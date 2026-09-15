package com.g42.platform.gms.customermerge.api.dto;

import lombok.Data;

/**
 * Hai hồ sơ trùng biển số nhưng là HAI NGƯỜI KHÁC NHAU (xe đã bán lại, người nhà mang xe đi...):
 * không gộp khách, chỉ gộp các dòng xe cùng biển số thành một và giao cho một chủ.
 * Lịch sử của người kia vẫn giữ nguyên trên hồ sơ của họ, chỉ trỏ vào đúng một xe.
 */
@Data
public class VehicleMergeRequest {
    private String plateKey;
    private Integer keepVehicleId;
    private Integer ownerCustomerId;
    private String note;
}
