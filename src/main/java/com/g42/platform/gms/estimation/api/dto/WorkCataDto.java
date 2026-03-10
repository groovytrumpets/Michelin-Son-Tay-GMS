package com.g42.platform.gms.estimation.api.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkCataDto {
    private Integer id;
    private String categoryCode;
    private String categoryName;
    private Integer displayOrder;
}
