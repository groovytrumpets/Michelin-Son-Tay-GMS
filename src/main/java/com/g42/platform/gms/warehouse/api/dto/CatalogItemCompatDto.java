package com.g42.platform.gms.warehouse.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Khai báo xe tương thích của một vật tư - hàng hóa: hãng xe / dòng xe / đời xe.
 * brandName và modelName chỉ dùng để hiển thị, không dùng khi ghi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogItemCompatDto {
    private Integer compatId;
    private Integer brandId;
    private String brandName;
    private Integer modelId;
    private String modelName;
    private Integer yearFrom;
    private Integer yearTo;
}
