package com.g42.platform.gms.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLookupResponse {
    private Integer customerId;
    private String phone;
    private String fullName;
    private String email;
    private boolean exists;
}
