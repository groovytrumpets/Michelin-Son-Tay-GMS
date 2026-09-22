package com.g42.platform.gms.service_ticket_management.api.dto.parts_sale;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ho so dung chung "Khach le" — man /parts-sales lay id nay de lap bao gia cho
 * luot ban khong lay thong tin khach. Ten nguoi mua that duoc nhap o form va luu
 * tren phieu, khong luu vao ho so nay.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalkInCustomerDto {
    private Integer customerId;
    private String fullName;
    private String customerCode;
}
