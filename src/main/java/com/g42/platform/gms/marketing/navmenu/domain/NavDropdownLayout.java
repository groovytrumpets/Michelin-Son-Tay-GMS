package com.g42.platform.gms.marketing.navmenu.domain;

/** Kiểu trình bày nội dung dropdown. */
public enum NavDropdownLayout {
    /** Danh sách dọc một cột — hợp với menu ít mục. */
    LIST,
    /** Chia thành nhiều cột theo {@code column_index} — hợp với menu nhiều mục. */
    COLUMNS,
    /** Thẻ có ảnh, tên và mô tả — dùng làm mega-menu giới thiệu. */
    CARDS
}
