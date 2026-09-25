package com.g42.platform.gms.branch.service;

import jakarta.persistence.PrePersist;

/**
 * Ghi xưởng lên lịch hẹn / phiếu mới nếu luồng tạo chưa tự đặt.
 *
 * <p>Có tới sáu chỗ tạo lịch hẹn / phiếu (khách đặt online, lễ tân tạo lịch, bán phụ tùng,
 * check-in, nhập bù...) và sẽ còn thêm; đặt ở tầng persist thì không luồng nào bị sót.
 * Luồng nào biết rõ xưởng (phiếu tạo từ lịch hẹn kế thừa xưởng của lịch) thì tự set trước,
 * listener thấy đã có giá trị sẽ để nguyên.
 */
public class BranchStampListener {

    @PrePersist
    public void stamp(Object entity) {
        if (!(entity instanceof BranchStamped stamped) || stamped.getBranchId() != null) {
            return;
        }
        BranchDirectory directory = BranchDirectory.instance();
        if (directory != null) {
            stamped.setBranchId(directory.resolveCurrent());
        }
    }
}
