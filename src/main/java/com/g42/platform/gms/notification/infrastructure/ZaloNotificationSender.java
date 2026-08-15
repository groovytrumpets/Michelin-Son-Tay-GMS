package com.g42.platform.gms.notification.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.notification.domain.NotificationSender;
import com.g42.platform.gms.notification.infrastructure.entity.ZaloToken;
import com.g42.platform.gms.notification.infrastructure.repository.ZaloTokenRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZaloNotificationSender implements NotificationSender {

    private static final String ZNS_URL = "https://business.openapi.zalo.me/message/template";

    private final ZaloTokenRepo zaloTokenRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean sendBookingRequested(String phone, String customerName, List<String> productName, String orderCode, String bookingStatus, String bookingTime, String garageLocation) {
        ZaloToken zaloToken = zaloTokenRepo.getZaloTokensByStateEqualsIgnoreCase("active");
        String accessToken = extractAccessToken(zaloToken, "booking requested " + orderCode);
        if (accessToken == null) {
            return false;
        }

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("customer_name", customerName);
        templateData.put("product_name", String.join(", ", productName));
        templateData.put("order_code", orderCode);
        templateData.put("booking_status", bookingStatus);
        templateData.put("booking_time", bookingTime);
        templateData.put("garage_location", garageLocation);

        return post(accessToken, "546766", phone, templateData, null, "booking requested " + orderCode);
    }

    @Transactional
    @Override
    public boolean sendBookingConfirm(String phone, String customerName, List<String> productName, String orderCode, LocalDateTime bookingTime, String garageLocation) {
        ZaloToken zaloToken = zaloTokenRepo.getZaloTokensByState("active");
        String accessToken = extractAccessToken(zaloToken, "confirm booking " + orderCode);
        if (accessToken == null) {
            return false;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        String formattedTime = bookingTime.format(formatter);
        String serviceNames;
        if (productName == null || productName.isEmpty()) {
            serviceNames = "Không có dịch vụ cụ thể"; // Zalo không cho phép để trống trường template
        } else {
            serviceNames = String.join(", ", productName);
        }

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("customer_name", customerName);
        templateData.put("service", serviceNames);
        templateData.put("booking_code", orderCode);
        templateData.put("booking_time", formattedTime);
        templateData.put("location", garageLocation);

        return post(accessToken, "562453", convertPhone(phone), templateData, null, "confirm booking " + orderCode);
    }

    @Override
    public boolean sendOtpVerify(String number, String otp) {
        ZaloToken zaloToken = zaloTokenRepo.getZaloTokenByState("active");
        String accessToken = extractAccessToken(zaloToken, "otp");
        if (accessToken == null) {
            return false;
        }

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("otp", otp);

        return post(accessToken, "547094", convertPhone(number), templateData, null, "otp");
    }

    @Override
    public boolean sendFeedback(String number, String name, String code) {
        ZaloToken zaloToken = zaloTokenRepo.getZaloTokenByState("active");
        String accessToken = extractAccessToken(zaloToken, "feedback " + code);
        if (accessToken == null) {
            return false;
        }

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("customer_name", name);
        templateData.put("service_code", code);

        return post(accessToken, "547146", convertPhone(number), templateData, code, "feedback " + code);
    }

    @Override
    public boolean sendEstimate(String number, String customerName, List<String> productName, String orderCode, LocalDateTime createAt, String garageLocation, String totalPrice) {
        ZaloToken zaloToken = zaloTokenRepo.getZaloTokenByState("active");
        String accessToken = extractAccessToken(zaloToken, "estimate " + orderCode);
        if (accessToken == null) {
            return false;
        }

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("customer_name", customerName);
        templateData.put("order_code", orderCode);
        templateData.put("time", createAt);
        templateData.put("service_price", productName);
        templateData.put("price", totalPrice);
        templateData.put("location", garageLocation);

        return post(accessToken, "574006", convertPhone(number), templateData, null, "estimate " + orderCode);
    }

    @Override
    public boolean sendBookingCf(String s, String nguyenVanA, String s1) {
        return false;
    }

    public static String convertPhone(String phone) {
        if (phone.startsWith("0")) {
            return "84" + phone.substring(1);
        }
        return phone;
    }

    /** Lấy access token đang hoạt động; trả null (kèm log) nếu chưa có token nào dùng được. */
    private String extractAccessToken(ZaloToken zaloToken, String context) {
        if (zaloToken == null || zaloToken.getAccessToken() == null || zaloToken.getAccessToken().isEmpty()) {
            log.warn("Zalo: chưa có access token hoạt động, không gửi được [{}]", context);
            return null;
        }
        return zaloToken.getAccessToken();
    }

    /**
     * Gọi API ZNS và đọc mã lỗi trong body.
     * Zalo trả HTTP 200 kể cả khi thất bại, phân biệt bằng trường "error" (0 = thành công).
     */
    private boolean post(String accessToken, String templateId, String phone,
                         Map<String, Object> templateData, String trackingId, String context) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access_token", accessToken);

        Map<String, Object> body = new HashMap<>();
        body.put("phone", phone);
        body.put("template_id", templateId);
        body.put("template_data", templateData);
        if (trackingId != null) {
            body.put("tracking_id", trackingId);
        }

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    ZNS_URL, new HttpEntity<>(body, headers), String.class);
            return isSuccess(response.getBody(), context);
        } catch (Exception e) {
            log.warn("Zalo: gửi [{}] thất bại: {}", context, e.getMessage());
            return false;
        }
    }

    private boolean isSuccess(String responseBody, String context) {
        if (responseBody == null || responseBody.isBlank()) {
            log.warn("Zalo: gửi [{}] không nhận được phản hồi", context);
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode errorNode = root.get("error");
            if (errorNode == null) {
                log.warn("Zalo: phản hồi [{}] không có trường error: {}", context, responseBody);
                return false;
            }
            if (errorNode.asInt(-1) == 0) {
                log.debug("Zalo: gửi [{}] thành công", context);
                return true;
            }
            log.warn("Zalo: gửi [{}] bị từ chối, error={}, message={}",
                    context, errorNode.asInt(-1), root.path("message").asText(""));
            return false;
        } catch (Exception e) {
            log.warn("Zalo: không đọc được phản hồi [{}]: {}", context, e.getMessage());
            return false;
        }
    }
}
