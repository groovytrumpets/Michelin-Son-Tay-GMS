package com.g42.platform.gms.customer.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kết quả tra cứu doanh nghiệp theo mã số thuế.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaxLookupDto {
    private String taxCode;
    private String name;
    private String shortName;
    private String internationalName;
    private String address;
}
