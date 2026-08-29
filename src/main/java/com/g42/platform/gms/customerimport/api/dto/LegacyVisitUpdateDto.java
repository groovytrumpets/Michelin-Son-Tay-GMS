package com.g42.platform.gms.customerimport.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Sửa tay một lượt đã nhập từ sổ cũ, ngay trên hồ sơ khách.
 *
 * Không có biển số: đổi xe của một lượt kéo theo chuyện tạo xe, chuyển chủ sở hữu —
 * đúng phần việc của luồng nhập lô, không nên làm lén trong một ô sửa nhanh. Muốn đổi
 * xe thì sửa lại cả lô ở màn nhập sổ cũ.
 *
 * Giảm giá vẫn đi theo quy ước của luồng nhập: một dòng có hạng mục "Giảm giá". Server
 * tự tách ra khỏi các dòng dịch vụ và tính lại tổng, y như lúc nhập file.
 */
@Getter
@Setter
@NoArgsConstructor
public class LegacyVisitUpdateDto {

    /** yyyy-MM-dd. Bắt buộc — đây là căn cứ tính lần cuối khách đến xưởng. */
    private String visitedDate;

    private String deliveredDate;
    private String legacyTicketCode;
    private Integer odometer;
    private String customerNote;
    private BigDecimal totalAmount;
    private String callNote;

    private List<ImportItemDto> items = new ArrayList<>();
}
