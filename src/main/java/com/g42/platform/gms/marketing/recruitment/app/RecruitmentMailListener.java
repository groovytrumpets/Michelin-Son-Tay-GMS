package com.g42.platform.gms.marketing.recruitment.app;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Kích hoạt việc gửi thư sau khi hồ sơ đã nằm chắc trong DB (AFTER_COMMIT) và
 * trên luồng nền (@Async) — form phía ứng viên không phải chờ máy chủ SMTP.
 *
 * <p>Để ở lớp riêng thay vì gắn thẳng vào {@link RecruitmentMailService}: gọi
 * một phương thức {@code @Transactional} của chính mình sẽ đi tắt qua proxy của
 * Spring, khi đó thư soạn xong thì hồ sơ lại không nạp nổi tin tuyển dụng liên
 * kết (lazy) vì đang ở ngoài giao dịch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitmentMailListener {

    private final RecruitmentMailService mailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationSubmitted(RecruitmentEvents.ApplicationSubmitted event) {
        try {
            mailService.notifyManagers(event.applicationId());
        } catch (Exception e) {
            // Lý do hỏng đã được ghi vào chính bản ghi hồ sơ bên trong
            // notifyManagers; ở đây chỉ chặn ngoại lệ thoát ra luồng nền.
            log.error("Tuyển dụng: gửi thư báo hồ sơ {} thất bại", event.applicationId(), e);
        }
        try {
            // Khối try riêng: ứng viên gõ sai email của chính mình không được
            // phép ảnh hưởng tới thư báo đã gửi cho quản lý ở trên.
            mailService.sendAcknowledgement(event.applicationId());
        } catch (Exception e) {
            log.error("Tuyển dụng: gửi thư cảm ơn cho hồ sơ {} thất bại", event.applicationId(), e);
        }
    }
}
