package com.g42.platform.gms.vehicle.service;

import com.g42.platform.gms.vehicle.dto.PlateOwnerDto;
import lombok.Getter;

import java.util.List;

/**
 * Biển số đang thuộc về (các) khách khác. KHÔNG phải lỗi cấm: xe dùng chung là chuyện bình
 * thường ở xưởng, nhưng gõ nhầm biển số cũng rất hay xảy ra — nên lần lưu đầu tiên trả lỗi
 * này kèm danh sách chủ hiện tại để nhân viên nhìn thấy rồi xác nhận
 * ({@code allowSharedPlate = true}) mới ghi.
 */
@Getter
public class SharedPlateException extends RuntimeException {

    public static final String CODE = "VEHICLE_PLATE_SHARED";

    private final transient List<PlateOwnerDto> owners;

    public SharedPlateException(String message, List<PlateOwnerDto> owners) {
        super(message);
        this.owners = owners;
    }
}
