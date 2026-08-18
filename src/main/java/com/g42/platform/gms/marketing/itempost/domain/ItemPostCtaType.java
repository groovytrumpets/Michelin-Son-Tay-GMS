package com.g42.platform.gms.marketing.itempost.domain;

/** Loại khối kêu gọi hành động chèn trong bài viết phụ tùng. */
public enum ItemPostCtaType {
    /** Nút dẫn sang trang đặt lịch. */
    BOOKING,
    /** Trỏ tới một mặt hàng trong catalog_item (dịch vụ hoặc phụ tùng). */
    CATALOG_ITEM,
    /** Trỏ tới một gói combo. */
    COMBO,
    /** Gọi hotline. */
    PHONE,
    /** Đường dẫn tự do do biên tập nhập. */
    EXTERNAL
}
