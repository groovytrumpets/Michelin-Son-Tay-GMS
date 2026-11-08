package com.g42.platform.gms.notification.api.controller;

import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.feedback.api.internal.FeedbackInternalApi;
import com.g42.platform.gms.notification.api.dto.EstimateRequest;
import com.g42.platform.gms.notification.api.dto.ZaloWebhookPayloadDto;
import com.g42.platform.gms.notification.application.service.NotificationService;
import com.g42.platform.gms.notification.application.zalo.ZaloWebhookEventLog;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/webhook/zalo")
public class ZaloWebhookController {
    @Autowired
    private FeedbackInternalApi feedbackInternalApi;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ZaloWebhookEventLog webhookEventLog;

    @PostMapping("/events")
    public ResponseEntity<String> receiveZaloEvents(
            @RequestBody ZaloWebhookPayloadDto payload,
            @RequestHeader(value = "X-ZEvent-Signature", required = false) String signature) {

        String eventName = payload == null ? null : payload.getEventName();
        log.info("Zalo webhook: {}", eventName);

        try {
            // 1. Kiểm tra xem có đúng là sự kiện đánh giá dịch vụ không
            if ("user_feedback".equals(eventName) && payload.getMessage() != null) {

                String trackingId = payload.getMessage().getTrackingId(); // Mã phiếu
                Integer rate = payload.getMessage().getRate(); // Số sao
                String note = payload.getMessage().getNote(); // Khách ghi chú gì

                feedbackInternalApi.addCusFeedbackRespond(rate,note,payload.getMessage().getFeedbacks(),payload.getMessage().getTrackingId(),payload.getMessage().getSubmitTime());
                webhookEventLog.record(eventName, "Phiếu " + trackingId + ": " + rate + " sao", true, null);
            } else {
                // Sự kiện khác (khách quan tâm OA, nhắn tin, trạng thái gửi ZNS...) — chưa xử lý,
                // chỉ ghi lại để màn /zalo-oa-config thấy webhook đang chạy.
                webhookEventLog.record(eventName, null, false, null);
            }

        } catch (Exception e) {
            // Log lỗi ra nhưng VẪN PHẢI TRẢ VỀ 200 OK cho Zalo
            log.warn("Lỗi xử lý Webhook Zalo [{}]: {}", eventName, e.getMessage());
            webhookEventLog.record(eventName, null, false, e.getMessage());
        }

        // 2. BẮT BUỘC TRẢ VỀ 200 OK
        return ResponseEntity.ok("Received");
    }
    @PostMapping("/estimate/sent")
    public ResponseEntity<String> estimate(@RequestBody EstimateRequest request){
        // Nằm dưới /api/webhook/** (permitAll) nên trước đây ai cũng gọi được — tức là gửi tin ZNS
        // tốn phí tới số bất kỳ. Chỉ nhân viên đăng nhập mới được gửi.
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.g42.platform.gms.auth.entity.StaffPrincipal)) {
            return ResponseEntity.status(401).body("Cần đăng nhập tài khoản nhân viên.");
        }
        if (request.getNumber() == null || request.getOrderCode() == null) {
            return ResponseEntity.badRequest().body("Thiếu thông tin số điện thoại hoặc mã đơn!");
        }
        LocalDateTime createAt = LocalDateTime.now();
        notificationService.sendEstimateViaZalo(
                request.getNumber(),
                request.getCustomerName(),
                request.getProductName(),
                request.getOrderCode(),
                createAt,
                request.getGarageLocation(),
                request.getTotalPrice()
        );
        return ResponseEntity.ok("Yêu cầu gửi báo giá Zalo ZNS đã được xử lý");
    }
}
