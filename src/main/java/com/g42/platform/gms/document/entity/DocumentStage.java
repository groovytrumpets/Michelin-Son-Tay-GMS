package com.g42.platform.gms.document.entity;

/**
 * Giai đoạn trong vòng đời phiếu mà chứng từ được in ra.
 *
 * Dùng để lọc danh sách mẫu hiện lên cho nhân viên: đứng ở màn tiếp nhận xe thì
 * chỉ thấy chứng từ giai đoạn INTAKE, đứng ở màn thu tiền thì chỉ thấy PAYMENT.
 * Không có tác dụng chặn — chỉ để danh sách chọn khỏi dài lê thê.
 */
public enum DocumentStage {

    /** Lúc tiếp nhận xe: hợp đồng sửa chữa, phiếu tiếp nhận. */
    INTAKE,

    /** Lúc chốt việc với khách: báo giá, phiếu kiểm tra xe. */
    QUOTE,

    /** Lúc giao xe/giao hàng: biên bản nghiệm thu, phiếu bảo hành. */
    HANDOVER,

    /** Lúc thu tiền: phiếu thanh toán, hoá đơn bán hàng. */
    PAYMENT,

    /** Nghiệp vụ kho, không gắn vào vòng đời phiếu dịch vụ. */
    WAREHOUSE,
}
