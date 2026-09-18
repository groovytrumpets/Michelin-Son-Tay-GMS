package com.g42.platform.gms.document.entity;

/**
 * Nguồn dữ liệu mà một dạng chứng từ lấy số liệu từ đó.
 *
 * Đây là thứ DUY NHẤT trong phân hệ biểu mẫu phải do lập trình viên định nghĩa:
 * mỗi nguồn ứng với một bộ trường cụ thể (khách hàng, xe, bảng hạng mục, tiền...)
 * và phải có code đi lấy đúng bộ đó khi in. Dạng chứng từ và bố cục thì người
 * dùng tự tạo bao nhiêu cũng được mà không cần đụng tới đây.
 *
 * Thêm một giá trị mới ở đây thì phải làm đủ ba việc, thiếu một là mẫu dựng ra
 * sẽ rỗng khi in:
 *   1. bổ sung bộ trường tương ứng trong fieldCatalog.js phía frontend,
 *   2. viết chỗ lấy dữ liệu thật khi in,
 *   3. bổ sung nhãn trong DATA_SOURCE_LABELS của frontend.
 */
public enum DataSourceType {

    /** Phiếu dịch vụ sửa chữa — khách, xe, báo giá, hạng mục, tiền. */
    SERVICE_TICKET,

    /** Phiếu bán phụ tùng lẻ — khách, danh sách hàng, tiền; không có xe. */
    PARTS_SALE,

    /** Phiếu nhập kho — người giao, kho nhận, danh sách hàng. */
    STOCK_ENTRY,

    /** Phiếu xuất kho — người nhận, lý do xuất, kho xuất, danh sách hàng. */
    STOCK_ISSUE,
}
