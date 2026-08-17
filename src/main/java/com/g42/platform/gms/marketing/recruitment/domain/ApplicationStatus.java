package com.g42.platform.gms.marketing.recruitment.domain;

/**
 * Trạng thái xử lý một hồ sơ ứng tuyển.
 *
 * <p>Đây là phễu tuyển dụng rút gọn cho quy mô một xưởng dịch vụ — không có
 * vòng phỏng vấn nhiều cấp, nên bốn bước giữa là đủ để biết hồ sơ đang nằm ở
 * đâu mà không bắt người tuyển bấm quá nhiều.
 */
public enum ApplicationStatus {
    /** Vừa nhận, chưa ai xem. */
    NEW,
    /** Đang xem xét hồ sơ. */
    REVIEWING,
    /** Đã hẹn / đang phỏng vấn. */
    INTERVIEW,
    /** Đã gửi đề nghị nhận việc. */
    OFFERED,
    /** Đã nhận việc. */
    HIRED,
    /** Không phù hợp. */
    REJECTED
}
