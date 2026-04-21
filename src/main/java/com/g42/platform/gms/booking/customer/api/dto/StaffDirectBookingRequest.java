package com.g42.platform.gms.booking.customer.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StaffDirectBookingRequest extends BaseBookingRequest {

    @Positive(message = "Estimate ID phai la so duong")
    private Integer estimateId;

    @NotBlank(message = "So dien thoai la bat buoc")
    @Pattern(regexp = "^0[0-9]{9,10}$", message = "So dien thoai phai bat dau bang 0 va co 10-11 chu so")
    private String phone;

    @NotBlank(message = "Ten khach hang la bat buoc")
    @Size(min = 2, max = 100, message = "Ten khach hang phai tu 2 den 100 ky tu")
    private String fullName;
}
