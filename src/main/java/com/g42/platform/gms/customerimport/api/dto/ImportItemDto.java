package com.g42.platform.gms.customerimport.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Một dòng dịch vụ trong phiếu cũ, đọc từ một khối 4 cột của sổ Excel. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportItemDto {

    /** Hạng mục gốc lấy từ hàng tiêu đề gộp: Lốp, Dầu động cơ, Phanh... */
    private String category;

    /** Ô "Diễn giải". Sổ cũ bỏ trống khá nhiều — khi đó lấy chính category làm tên. */
    private String itemName;

    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
