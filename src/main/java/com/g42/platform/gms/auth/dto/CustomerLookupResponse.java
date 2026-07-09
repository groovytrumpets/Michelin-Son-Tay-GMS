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
