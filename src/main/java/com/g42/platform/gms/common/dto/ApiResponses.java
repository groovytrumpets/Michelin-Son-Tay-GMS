package com.g42.platform.gms.common.dto;

public class ApiResponses {

    // 1. Dùng cho API có data trả về (Giữ nguyên)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", "Thành công", data);
    }

    // 2. Dùng cho API chỉ có thông báo (Sửa lại kiểu trả về là Void)
    public static ApiResponse<Void> successMessage(String message) {
        // Tham số thứ 4 (data) truyền vào là null
        return new ApiResponse<>(true, "SUCCESS", message, null);
    }

    // 3. Dùng cho API lỗi (Giữ nguyên hoặc sửa thành Void nếu muốn)
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}