package com.g42.platform.gms.common.dto;

public class ApiResponses {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", null, data);
    }

    public static ApiResponse<?> successMessage(String message) {
        return new ApiResponse<>(true, "SUCCESS", message, null);
    }

    public static ApiResponse<?> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
