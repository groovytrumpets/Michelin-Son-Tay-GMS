package com.g42.platform.gms.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CustomerLookupResponse {
    private Integer customerId;
    private String phone;
    private String fullName;
    private String email;
    private boolean exists;
    private Long serviceUsageCount = 0L;

    /**
     * Khi tra theo biển số: TẤT CẢ hồ sơ đang gắn biển số đó. Một biển số dùng chung được cho
     * nhiều khách (vợ chồng, gia đình, công ty — changeset 039), nên màn hình phải cho lễ tân
     * chọn đúng người thay vì âm thầm lấy hồ sơ đầu tiên. Có đúng 1 chủ thì danh sách có 1 phần tử.
     */
    private java.util.List<PlateOwner> plateOwners = new java.util.ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlateOwner {
        private Integer customerId;
        private String fullName;
        private String phone;
        private String email;
        private Integer vehicleId;
        private String licensePlate;
    }

    public CustomerLookupResponse(Integer customerId, String phone, String fullName, String email, boolean exists) {
        this.customerId = customerId;
        this.phone = phone;
        this.fullName = fullName;
        this.email = email;
        this.exists = exists;
        this.serviceUsageCount = 0L;
    }

    public CustomerLookupResponse(Integer customerId, String phone, String fullName, String email, boolean exists, Long serviceUsageCount) {
        this.customerId = customerId;
        this.phone = phone;
        this.fullName = fullName;
        this.email = email;
        this.exists = exists;
        this.serviceUsageCount = serviceUsageCount;
    }
}
