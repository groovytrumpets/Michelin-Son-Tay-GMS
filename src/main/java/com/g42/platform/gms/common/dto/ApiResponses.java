package com.g42.platform.gms.common.dto;

public class ApiResponses {

    // 1. Dùng cho API có data trả về
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", "Thành công", data);
    }

    // 2. Dùng cho API có data + custom message
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, "SUCCESS", message, data);
    }

    // 3. Dùng cho API chỉ có thông báo (không có data)
    public static ApiResponse<Void> successMessage(String message) {
        return new ApiResponse<>(true, "SUCCESS", message, null);
    }

    // 4. Dùng cho API lỗi
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }

    /**
     * Lỗi kèm dữ liệu để phía gọi xử lý tiếp, VD biển số xe đã thuộc về khách khác thì trả
     * kèm danh sách chủ xe hiện tại để màn hình hỏi "vẫn thêm vào hồ sơ này chứ?".
     */
    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }

    public static ApiResponse<?> errorBill(String code, String message) {
        return new ApiResponse<>(true, code, message, null);
    }
}
