package com.g42.platform.gms.notification.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.notification.application.zalo.ZaloOaSettings;
import com.g42.platform.gms.notification.application.zalo.ZaloTokenService;
import com.g42.platform.gms.notification.domain.NotificationSender;
import com.g42.platform.gms.notification.domain.ZnsTemplateType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gửi thông báo qua Zalo ZNS.
 *
 * Mọi hàm send* trả về true khi Zalo xác nhận gửi thành công (body có "error": 0),
 * false khi thiếu access token, lỗi mạng, hoặc Zalo trả về mã lỗi — nhờ đó
 * CustomerNotificationDispatcher biết để chuyển sang gửi email thay thế.
 *
 * Token lấy qua ZaloTokenService (chịu được lỡ có nhiều dòng active), mã mẫu lấy qua
 * ZaloOaSettings (đổi được ở /zalo-oa-config, mặc định là mã cứng cũ).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZaloNotificationSender implements NotificationSender {

    private static final String ZNS_URL = "https://business.openapi.zalo.me/message/template";
    private static final DateTimeFormatter BOOKING_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final ZaloTokenService tokenService;
    private final ZaloOaSettings settings;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    /** Kết quả chi tiết của 1 lần gửi — màn cấu hình cần thấy mã lỗi Zalo, không chỉ true/false. */
    public record ZnsSendResult(boolean ok, Integer errorCode, String message, String msgId, String templateId) {}

    // ------------------------------------------------------------------ dữ liệu từng mẫu

    private static Map<String, Object> bookingRequestedData(String customerName, List<String> productName, String orderCode,
                                                           String bookingStatus, String bookingTime, String garageLocation) {
        Map<String, Object> d = new HashMap<>();
        d.put("customer_name", customerName);
        d.put("product_name", productName == null ? "" : String.join(", ", productName));
        d.put("order_code", orderCode);
        d.put("booking_status", bookingStatus);
        d.put("booking_time", bookingTime);
        d.put("garage_location", garageLocation);
        return d;
    }

    private static Map<String, Object> bookingConfirmData(String customerName, List<String> productName, String orderCode,
                                                         LocalDateTime bookingTime, String garageLocation) {
        String serviceNames = productName == null || productName.isEmpty()
                ? "Không có dịch vụ cụ thể" // Zalo không cho phép để trống trường template
                : String.join(", ", productName);
        Map<String, Object> d = new HashMap<>();
        d.put("customer_name", customerName);
        d.put("service", serviceNames);
        d.put("booking_code", orderCode);
        d.put("booking_time", bookingTime.format(BOOKING_TIME));
        d.put("location", garageLocation);
        return d;
    }

    private static Map<String, Object> otpData(String otp) {
        Map<String, Object> d = new HashMap<>();
        d.put("otp", otp);
        return d;
    }

    private static Map<String, Object> feedbackData(String name, String code) {
        Map<String, Object> d = new HashMap<>();
        d.put("customer_name", name);
        d.put("service_code", code);
        return d;
    }

    private static Map<String, Object> estimateData(String customerName, List<String> productName, String orderCode,
                                                   LocalDateTime createAt, String garageLocation, String totalPrice) {
        Map<String, Object> d = new HashMap<>();
        d.put("customer_name", customerName);
        d.put("order_code", orderCode);
        d.put("time", createAt);
        d.put("service_price", productName);
        d.put("price", totalPrice);
        d.put("location", garageLocation);
        return d;
    }

    // ------------------------------------------------------------------ gửi thật

    public boolean sendBookingRequested(String phone, String customerName, List<String> productName, String orderCode,
                                        String bookingStatus, String bookingTime, String garageLocation) {
        return send(ZnsTemplateType.BOOKING_REQUESTED, phone,
                bookingRequestedData(customerName, productName, orderCode, bookingStatus, bookingTime, garageLocation),
                null, "booking requested " + orderCode).ok();
    }

    @Override
    public boolean sendBookingConfirm(String phone, String customerName, List<String> productName, String orderCode,
                                      LocalDateTime bookingTime, String garageLocation) {
        return send(ZnsTemplateType.BOOKING_CONFIRM, phone,
                bookingConfirmData(customerName, productName, orderCode, bookingTime, garageLocation),
                null, "confirm booking " + orderCode).ok();
    }

    @Override
    public boolean sendOtpVerify(String number, String otp) {
        return send(ZnsTemplateType.OTP, number, otpData(otp), null, "otp").ok();
    }

    @Override
    public boolean sendFeedback(String number, String name, String code) {
        // tracking_id = mã phiếu: webhook user_feedback trả lại đúng mã này để gắn đánh giá vào phiếu.
        return send(ZnsTemplateType.FEEDBACK, number, feedbackData(name, code), code, "feedback " + code).ok();
    }

    @Override
    public boolean sendEstimate(String number, String customerName, List<String> productName, String orderCode,
                                LocalDateTime createAt, String garageLocation, String totalPrice) {
        return send(ZnsTemplateType.ESTIMATE, number,
                estimateData(customerName, productName, orderCode, createAt, garageLocation, totalPrice),
                null, "estimate " + orderCode).ok();
    }

    @Override
    public boolean sendBookingCf(String s, String nguyenVanA, String s1) {
        return false;
    }

    /** Gửi thử 1 tin với dữ liệu mẫu (nút "Gửi thử" ở /zalo-oa-config). Tốn phí như tin thật. */
    public ZnsSendResult sendTest(ZnsTemplateType type, String phone) {
        String code = "TEST-" + System.currentTimeMillis() % 100000;
        LocalDateTime now = LocalDateTime.now();
        List<String> items = List.of("Thay dầu động cơ (tin thử)");
        String garage = "Michelin Sơn Tây";
        Map<String, Object> data = switch (type) {
            case BOOKING_REQUESTED -> bookingRequestedData("Khách thử", items, code, "Đang chờ xác nhận",
                    now.format(BOOKING_TIME), garage);
            case BOOKING_CONFIRM -> bookingConfirmData("Khách thử", items, code, now, garage);
            case OTP -> otpData("123456");
            case FEEDBACK -> feedbackData("Khách thử", code);
            case ESTIMATE -> estimateData("Khách thử", items, code, now, garage, "500.000");
        };
        // Không gắn tracking_id: đánh giá của tin thử không được trùng mã một phiếu thật.
        return send(type, phone, data, null, "test " + type);
    }

    public static String convertPhone(String phone) {
        if (phone == null) return null;
        String p = phone.replaceAll("[\\s.\\-]", "");
        if (p.startsWith("+")) p = p.substring(1);
        if (p.startsWith("0")) {
            return "84" + p.substring(1);
        }
        return p;
    }

    /**
     * Gọi API ZNS và đọc mã lỗi trong body.
     * Zalo trả HTTP 200 kể cả khi thất bại, phân biệt bằng trường "error" (0 = thành công).
     */
    private ZnsSendResult send(ZnsTemplateType type, String phone, Map<String, Object> templateData,
                               String trackingId, String context) {
        String templateId = settings.templateId(type);
        String accessToken = tokenService.currentAccessToken();
        if (accessToken == null) {
            log.warn("Zalo: chưa có access token hoạt động, không gửi được [{}]", context);
            return new ZnsSendResult(false, null, "Chưa có access token Zalo OA", null, templateId);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access_token", accessToken);

        Map<String, Object> body = new HashMap<>();
        body.put("phone", convertPhone(phone));
        body.put("template_id", templateId);
        body.put("template_data", templateData);
        if (trackingId != null) {
            body.put("tracking_id", trackingId);
        }

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    ZNS_URL, new HttpEntity<>(body, headers), String.class);
            return readResult(response.getBody(), context, templateId);
        } catch (Exception e) {
            log.warn("Zalo: gửi [{}] thất bại: {}", context, e.getMessage());
            return new ZnsSendResult(false, null, "Không gọi được Zalo: " + e.getMessage(), null, templateId);
        }
    }

    private ZnsSendResult readResult(String responseBody, String context, String templateId) {
        if (responseBody == null || responseBody.isBlank()) {
            log.warn("Zalo: gửi [{}] không nhận được phản hồi", context);
            return new ZnsSendResult(false, null, "Zalo không phản hồi", null, templateId);
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode errorNode = root.get("error");
            if (errorNode == null) {
                log.warn("Zalo: phản hồi [{}] không có trường error: {}", context, responseBody);
                return new ZnsSendResult(false, null, "Phản hồi lạ từ Zalo", null, templateId);
            }
            int error = errorNode.asInt(-1);
            String message = root.path("message").asText("");
            if (error == 0) {
                log.debug("Zalo: gửi [{}] thành công", context);
                return new ZnsSendResult(true, 0, message, root.path("data").path("msg_id").asText(null), templateId);
            }
            log.warn("Zalo: gửi [{}] bị từ chối, error={}, message={}", context, error, message);
            return new ZnsSendResult(false, error, message, null, templateId);
        } catch (Exception e) {
            log.warn("Zalo: không đọc được phản hồi [{}]: {}", context, e.getMessage());
            return new ZnsSendResult(false, null, "Không đọc được phản hồi Zalo", null, templateId);
        }
    }
}
