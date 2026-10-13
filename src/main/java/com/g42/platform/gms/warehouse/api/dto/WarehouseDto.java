package com.g42.platform.gms.warehouse.api.dto;

import com.g42.platform.gms.common.enums.WarehouseTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDto {
    private Integer warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private WarehouseTypeEnum warehouseType;
    private Integer parentWarehouseId;
    private String address;
    private Integer managerStaffId;
    private Boolean isActive;
    private Instant createdAt;

    // ── Chỉ có khi gọi /warehouse/all?withStock=true (null ở các chỗ khác) ──
    /** Tổng số lượng hàng nằm trực tiếp trong kho này. */
    private BigDecimal ownQuantity;
    /** Tồn cộng dồn = hàng của chính kho + mọi kho con cháu (không tính kho lỗi). */
    private BigDecimal totalQuantity;
    /** Số mặt hàng còn tồn (> 0) nằm trực tiếp trong kho này. */
    private Integer ownItemCount;
    /** Số mặt hàng khác nhau còn tồn trong cả cây kho (một mặt hàng ở nhiều kho con chỉ đếm 1). */
    private Integer totalItemCount;
}
