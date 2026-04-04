package com.g42.platform.gms.notification.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.notification.api.dto.ZaloWebhookPayloadDto;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/webhook/zalo")
public class ZaloWebhookController {
    @PostMapping("/events")
    public ResponseEntity<String> receiveZaloEvents(
            @RequestBody ZaloWebhookPayloadDto payload,
            @RequestHeader(value = "X-ZEvent-Signature", required = false) String signature) {

        System.out.println("========== ĐÃ NHẬN WEBHOOK TỪ ZALO ==========");
        System.out.println("Sự kiện: " + payload.getEventName());

        try {
            try {
                // Dùng ObjectMapper để in JSON ra cho đẹp, dễ nhìn dễ đọc
                ObjectMapper mapper = new ObjectMapper();
                String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
                System.out.println(prettyJson);
            } catch (Exception e) {
                System.out.println(payload);
            }

        } catch (Exception e) {
            // Log lỗi ra nhưng VẪN PHẢI TRẢ VỀ 200 OK cho Zalo
            System.err.println("Lỗi xử lý Webhook Zalo: " + e.getMessage());
        }

        // 2. BẮT BUỘC TRẢ VỀ 200 OK
        return ResponseEntity.ok("Received");
    }
}
