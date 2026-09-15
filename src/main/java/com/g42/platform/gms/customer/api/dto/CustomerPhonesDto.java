package com.g42.platform.gms.customer.api.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Toàn bộ số điện thoại của một khách: số chính (đăng nhập, nhận Zalo) + các số phụ.
 * Dùng cho cả đọc lẫn ghi đè cả danh sách ở PUT /api/admin/customer/{id}/phones.
 */
@Data
public class CustomerPhonesDto {

    private Integer customerId;

    /** Số chính — ghi vào customer_profile.phone. */
    private String primaryPhone;

    private List<OtherPhone> otherPhones = new ArrayList<>();

    @Data
    public static class OtherPhone {
        private Integer customerPhoneId;
        private String phone;
        private String note;
    }
}
