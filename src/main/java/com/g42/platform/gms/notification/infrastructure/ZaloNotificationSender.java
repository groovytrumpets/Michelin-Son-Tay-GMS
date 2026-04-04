package com.g42.platform.gms.notification.infrastructure;

import com.g42.platform.gms.notification.domain.NotificationSender;
import com.g42.platform.gms.notification.infrastructure.entity.ZaloToken;
import com.g42.platform.gms.notification.infrastructure.repository.ZaloTokenRepo;
import org.springframework.beans.factory.annotation.Autowired;
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

@Component
public class ZaloNotificationSender implements NotificationSender {
    @Autowired
    private ZaloTokenRepo zaloTokenRepo;
    public void sendBookingRequested(String phone, String customerName, List<String> productName, String orderCode, String bookingStatus, String bookingTime, String garageLocation) {
        String template_id = "546766";
        String url = "https://business.openapi.zalo.me/message/template";
        ZaloToken zaloToken = zaloTokenRepo.getZaloTokensByStateEqualsIgnoreCase("active");
//        String accessToken = zaloToken.getAccessToken();

        String templateId = "546766";

        RestTemplate restTemplate = new RestTemplate();

        // Header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access_token", zaloToken.getAccessToken());

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
        ZaloToken zaloToken = zaloTokenRepo.getZaloTokenByState("active");
//        String accessToken = zaloToken.getAccessToken();

        String templateId = "546916";

        RestTemplate restTemplate = new RestTemplate();


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access_token", zaloToken.getAccessToken());


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
    public void sendOtpVerify(String number, String otp) {
        String url = "https://business.openapi.zalo.me/message/template";
        ZaloToken zaloToken = zaloTokenRepo.getZaloTokenByState("active");
//        String accessToken = zaloToken.getAccessToken();

        String templateId = "547094";

        RestTemplate restTemplate = new RestTemplate();


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access_token", zaloToken.getAccessToken());


        Map<String, Object> body = new HashMap<>();
        body.put("phone", convertPhone(number));
        body.put("template_id", templateId);

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("otp", otp);
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
    public void sendFeedback(String number, String name, String code) {
        String url = "https://business.openapi.zalo.me/message/template";
        ZaloToken zaloToken = zaloTokenRepo.getZaloTokenByState("active");
//        String accessToken = zaloToken.getAccessToken();

        String templateId = "547146";

        RestTemplate restTemplate = new RestTemplate();


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access_token", zaloToken.getAccessToken());


        Map<String, Object> body = new HashMap<>();
        body.put("phone", convertPhone(number));
        body.put("template_id", templateId);

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("customer_name", name);
        templateData.put("service_code",code);
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
