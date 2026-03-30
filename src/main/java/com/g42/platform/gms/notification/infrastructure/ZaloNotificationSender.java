package com.g42.platform.gms.notification.infrastructure;

import com.g42.platform.gms.notification.domain.NotificationSender;
import com.g42.platform.gms.notification.infrastructure.entity.ZaloToken;
import com.g42.platform.gms.notification.infrastructure.repository.ZaloTokenRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ZaloNotificationSender implements NotificationSender {
    private final String ZALO_TOKEN = "wBmhQvCHKmJjl2fhbXTI1Ew_8csL3Yr3xjWRKlK136l9mJqAw3TC4yoUDHpbTranqjXF8VuaMoRBwNmVqneC9Td08plaRremwgLK6SHkUNZDka9ki50t3i-77cN4S0rqwveROyfJ7dxjlnPk-aWs0ywJ7XokP6e-fRyQAjf6R33vd5iypZCk3ExWC1_wD1Otplmd0TKmAo3Q-Zv9nHmhAR2m2nU8I0aQiuOk9gSk0n-7w2KflXqeBfpLK4Ib24HfgzrcSxKgR42q_NHhcZHiMQxWL4sL5oDMkyeILATZ7JkoZZuYlaupFucMIagAM4buWV5fTAuAM4QIpt1CZcL9UfB3VaQl4bforU9SJU1GK5hdkMTrxcrNHk-8VrZzOc5o_izu78GUO06JwN4fdpX50vN3QZfJR4sVGLYH3Li2";

    private ZaloTokenRepo zaloTokenRepo;
    public void sendBookingRequested(String phone, String customerName, List<String> productName, String orderCode, String bookingStatus, String bookingTime, String garageLocation) {
        String template_id = "546766";
        String url = "https://business.openapi.zalo.me/message/template";
//        ZaloToken zaloToken = zaloTokenRepo.getZaloTokenByState("verified");
//        String accessToken = zaloToken.getAccessToken();

        String templateId = "546766";

        RestTemplate restTemplate = new RestTemplate();

        // Header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access_token", ZALO_TOKEN);

        // Body
        Map<String, Object> body = new HashMap<>();

        body.put("phone", phone);
        body.put("template_id", templateId);

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("customer_name", customerName);
        templateData.put("product_name", String.join(", ", productName));
        templateData.put("order_code", orderCode);
        templateData.put("booking_status", bookingStatus);
        templateData.put("booking_time", bookingTime);
        templateData.put("garage_location", garageLocation);

        body.put("template_data", templateData);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                url,
                request,
                String.class
        );

        System.out.println(response.getBody());
    }
    public void sendBookingConfirm(String phone, String customerName, List<String> productName, String orderCode, LocalDateTime bookingTime, String garageLocation) {
        String template_id = "546916";
        String url = "https://business.openapi.zalo.me/message/template";
//        ZaloToken zaloToken = zaloTokenRepo.getZaloTokenByState("verified");
//        String accessToken = zaloToken.getAccessToken();

        String templateId = "546916";

        RestTemplate restTemplate = new RestTemplate();


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access_token", ZALO_TOKEN);


        Map<String, Object> body = new HashMap<>();
        body.put("phone", convertPhone(phone));
        body.put("template_id", templateId);

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

        String formattedTime = bookingTime.format(formatter);

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("customer_name", customerName);
        templateData.put("service", String.join(", ", productName));
        templateData.put("booking_code", orderCode);
        templateData.put("booking_time", formattedTime);
        templateData.put("location", garageLocation);

        body.put("template_data", templateData);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                url,
                request,
                String.class
        );

        System.out.println(response.getBody());
    }

    @Override
    public void sendBookingCf(String s, String nguyenVanA, String s1) {

    }
    public static String convertPhone(String phone) {
        if (phone.startsWith("0")) {
            return "84" + phone.substring(1);
        }
        return phone;
    }
}
