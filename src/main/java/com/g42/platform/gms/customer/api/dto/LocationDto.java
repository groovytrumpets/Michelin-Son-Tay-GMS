package com.g42.platform.gms.customer.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một đơn vị hành chính (tỉnh/thành, quận/huyện, xã/phường).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    private String id;
    private String name;
}
