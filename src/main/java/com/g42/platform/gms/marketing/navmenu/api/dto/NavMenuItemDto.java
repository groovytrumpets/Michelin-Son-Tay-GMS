package com.g42.platform.gms.marketing.navmenu.api.dto;

import com.g42.platform.gms.marketing.navmenu.domain.NavDropdownLayout;
import com.g42.platform.gms.marketing.navmenu.domain.NavItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Kiểu dữ liệu vào/ra của phân hệ menu điều hướng. */
public final class NavMenuItemDto {

    private NavMenuItemDto() {
    }

    /**
     * Một mục kèm các mục con của nó. Dùng chung cho trang khách và màn quản trị:
     * trang khách chỉ nhận mục đang bật, màn quản trị nhận cả mục đang tắt.
     */
    public record ItemDto(
            Integer navItemId,
            Integer parentId,
            NavItemType itemType,
            NavDropdownLayout dropdownLayout,
            String label,
            String description,
            String imageUrl,
            String badgeText,
            String targetPath,
            Boolean openInNewTab,
            Integer columnIndex,
            Integer displayOrder,
            Boolean isActive,
            List<ItemDto> children
    ) {
    }

    public record SaveRequest(
            @NotNull(message = "Chưa chọn loại mục menu")
            NavItemType itemType,
            NavDropdownLayout dropdownLayout,
            @NotBlank(message = "Tên hiển thị không được để trống")
            @Size(max = 100, message = "Tên hiển thị tối đa 100 ký tự")
            String label,
            @Size(max = 255) String description,
            @Size(max = 500) String imageUrl,
            @Size(max = 30) String badgeText,
            @Size(max = 500) String targetPath,
            Boolean openInNewTab,
            Integer columnIndex,
            Boolean isActive,
            /** Rỗng nghĩa là mục cấp 1 nằm thẳng trên thanh menu. */
            Integer parentId
    ) {
    }

    /**
     * Kết quả sau một thao tác kéo thả: mỗi dòng cho biết mục đó thuộc mục cha
     * nào, ở cột nào và đứng thứ mấy. Gửi cả cây trong một lần để tránh trạng
     * thái nửa vời khi có nhiều mục cùng đổi chỗ.
     */
    public record ReorderEntry(
            @NotNull Integer navItemId,
            Integer parentId,
            Integer columnIndex,
            @NotNull Integer displayOrder
    ) {
    }

    public record ReorderRequest(List<ReorderEntry> items) {
    }
}
