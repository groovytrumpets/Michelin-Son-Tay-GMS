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
    private String color;
    /**
     * Danh mục là tùy chọn — để trống thì phụ tùng không thuộc danh mục nào.
     * Trước đây mặc định là 1, nay không được phép vì danh mục #1 có thể không tồn tại
     * và sẽ làm vỡ khóa ngoại.
     */
    private Integer itemCategoryId;

    private Integer brandId;
    private Integer productLineId;

}
