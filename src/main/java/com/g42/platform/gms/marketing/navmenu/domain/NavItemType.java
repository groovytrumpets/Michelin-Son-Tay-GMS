package com.g42.platform.gms.marketing.navmenu.domain;

/**
 * Loại một mục trên thanh điều hướng.
 *
 * <p>Có hai nhóm dropdown khác nhau về nguồn dữ liệu:
 * <ul>
 *   <li>{@link #CUSTOM_MENU} — nội dung do người quản trị tự dựng, lưu thành các
 *       bản ghi con trong chính bảng {@code nav_menu_item}.</li>
 *   <li>Các loại {@code AUTO_*} — nội dung sinh từ nghiệp vụ (danh mục phụ tùng,
 *       hãng xe, danh mục tin tức). Không lưu mục con, nhờ vậy menu luôn khớp với
 *       dữ liệu thật thay vì trôi dần mỗi khi nghiệp vụ thay đổi.</li>
 * </ul>
 */
public enum NavItemType {
    /** Liên kết nội bộ, điều hướng phía trình duyệt không tải lại trang. */
    LINK,
    /** Liên kết ra ngoài trang, luôn dùng thẻ {@code <a>} thường. */
    EXTERNAL,
    /** Hành vi đặc biệt: cuộn tới khối liên hệ ở trang chủ thay vì điều hướng. */
    SCROLL_CONTACT,
    /** Tiêu đề nhóm bên trong dropdown, không bấm được. */
    HEADING,
    /** Dropdown do quản trị tự dựng từ các mục con. */
    CUSTOM_MENU,
    /** Dropdown danh mục phụ tùng, lấy từ catalog. */
    AUTO_PARTS_CATEGORIES,
    /** Dropdown phụ tùng theo hãng xe / dòng xe. */
    AUTO_PARTS_BY_VEHICLE,
    /** Dropdown danh mục tin tức, lấy từ bảng {@code post_category}. */
    AUTO_NEWS_CATEGORIES;

    /** Mục này có mở dropdown hay không (dù nội dung tự dựng hay tự sinh). */
    public boolean hasDropdown() {
        return this == CUSTOM_MENU
                || this == AUTO_PARTS_CATEGORIES
                || this == AUTO_PARTS_BY_VEHICLE
                || this == AUTO_NEWS_CATEGORIES;
    }
}
