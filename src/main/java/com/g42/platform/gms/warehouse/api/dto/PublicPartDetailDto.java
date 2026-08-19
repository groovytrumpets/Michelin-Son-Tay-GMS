package com.g42.platform.gms.warehouse.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Các trường an toàn để hiển thị công khai ở trang chi tiết sản phẩm bán hàng
 * (khách vãng lai, không cần đăng nhập). KHÔNG chứa costPrice, tồn kho theo lô,
 * hay bất kỳ dữ liệu nội bộ nào — khác với CatalogDetailDto dùng cho nhân viên.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicPartDetailDto {
    private String origin;
    private String productLine;
    private String unit;
    private List<ItemColorDto> colors;
    private List<SpecificationRespondDto> specifications;
}
