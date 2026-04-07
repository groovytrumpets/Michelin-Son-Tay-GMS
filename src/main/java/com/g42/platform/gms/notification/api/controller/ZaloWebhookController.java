package com.g42.platform.gms.notification.api.controller;

import com.g42.platform.gms.feedback.api.internal.FeedbackInternalApi;
import com.g42.platform.gms.notification.api.dto.ZaloWebhookPayloadDto;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/webhook/zalo")
public class ZaloWebhookController {
    @Autowired
    private FeedbackInternalApi feedbackInternalApi;
    @PostMapping("/events")
    public ResponseEntity<String> receiveZaloEvents(
            @RequestBody ZaloWebhookPayloadDto payload,
            @RequestHeader(value = "X-ZEvent-Signature", required = false) String signature) {

        System.out.println("========== ĐÃ NHẬN WEBHOOK TỪ ZALO ==========");
        System.out.println("Sự kiện: " + payload.getEventName());

        try {
            // 1. Kiểm tra xem có đúng là sự kiện đánh giá dịch vụ không
            if ("user_feedback".equals(payload.getEventName())) {

                String trackingId = payload.getMessage().getTrackingId(); // Mã phiếu
                Integer rate = payload.getMessage().getRate(); // Số sao
                String note = payload.getMessage().getNote(); // Khách ghi chú gì


                System.out.printf("Khách hàng đánh giá Phiếu %s: %d Sao. Ghi chú: %s%n", trackingId, rate, note);

                // TODO: Gọi Service để lưu vào bảng Feedback trong Database
                feedbackInternalApi.addCusFeedbackRespond(rate,note,payload.getMessage().getFeedbacks(),payload.getMessage().getTrackingId(),payload.getMessage().getSubmitTime());
            }

            // Nếu có các event khác (như báo gửi tin thành công/thất bại) thì dùng else if ở đây

        } catch (Exception e) {
            // Log lỗi ra nhưng VẪN PHẢI TRẢ VỀ 200 OK cho Zalo
            System.err.println("Lỗi xử lý Webhook Zalo: " + e.getMessage());
        }

        // 2. BẮT BUỘC TRẢ VỀ 200 OK
        return ResponseEntity.ok("Received");
    }
}
