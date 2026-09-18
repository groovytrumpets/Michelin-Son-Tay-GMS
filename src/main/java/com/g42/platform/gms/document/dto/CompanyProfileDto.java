package com.g42.platform.gms.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyProfileDto {
    private String name;
    private String shortName;
    private String taxCode;
    private String address;
    private String representative;
    private String representativeTitle;
    private String phone;
    private String email;
    private String website;
    private String bankAccounts;
    private String logoUrl;
}
