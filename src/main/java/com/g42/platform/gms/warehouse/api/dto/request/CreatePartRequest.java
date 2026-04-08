package com.g42.platform.gms.warehouse.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePartRequest {

    @NotBlank
    private String itemName;

    /** Kho sẽ tạo inventory record (qty=0) khi tạo part mới */
    @NotNull
    private Integer warehouseId;

    /** SKU tự sinh nếu để trống */
    private String sku;

    private String partNumber;
    private String barcode;
    private String unit;
    private String description;
    private String madeIn;

    /** work_category_id — mặc định 1 nếu không truyền */
    private Integer workCategoryId = 1;

    private Integer brandId;
    private Integer productLineId;

}
