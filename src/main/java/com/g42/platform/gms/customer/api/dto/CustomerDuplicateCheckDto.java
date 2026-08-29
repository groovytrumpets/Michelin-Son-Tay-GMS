package com.g42.platform.gms.customer.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kết quả tra trùng định danh khách hàng (số điện thoại / email) cho màn hình
 * danh bạ khách hàng. Trả kèm hồ sơ đang giữ định danh đó để nhân viên biết
 * phải mở hồ sơ nào thay vì tạo hồ sơ mới.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDuplicateCheckDto {
    private boolean phoneTaken;
    private Integer phoneCustomerId;
    private String phoneCustomerName;
    private String phoneCustomerCode;

    private boolean emailTaken;
    private Integer emailCustomerId;
    private String emailCustomerName;
    private String emailCustomerCode;
}
