package com.g42.platform.gms.customermerge.api.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gộp các hồ sơ mergeCustomerIds VÀO keepCustomerId. Hồ sơ bị gộp sẽ bị xoá sau khi toàn bộ
 * dữ liệu (phiếu, lịch hẹn, xe, điểm, số điện thoại...) đã chuyển sang hồ sơ giữ lại.
 */
@Data
public class CustomerMergeRequest {

    private Integer keepCustomerId;
    private List<Integer> mergeCustomerIds;

    /** key trường (CustomerCompareDto.Field.key) → lấy giá trị từ hồ sơ nào. Thiếu key = mặc định. */
    private Map<String, Integer> fieldSources = new HashMap<>();

    /** Ghi chú: true (mặc định) = nối ghi chú của mọi hồ sơ; false = lấy theo fieldSources["note"]. */
    private Boolean combineNotes;

    /** Số chính sau khi gộp. Mọi số khác vẫn giữ lại làm số phụ. Bỏ trống = số chính của hồ sơ giữ lại. */
    private String primaryPhone;

    /** Giữ tài khoản đăng nhập (PIN, trạng thái) của hồ sơ nào. Bỏ trống = tự chọn tài khoản đang dùng được. */
    private Integer accountFromCustomerId;

    /** plateKey → vehicleId được giữ khi cùng biển số có nhiều dòng xe. Bỏ trống = xe của hồ sơ giữ lại. */
    private Map<String, Integer> keepVehicleIds = new HashMap<>();

    private String note;
}
