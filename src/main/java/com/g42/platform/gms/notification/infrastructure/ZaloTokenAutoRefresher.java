package com.g42.platform.gms.notification.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.notification.infrastructure.entity.ZaloToken;
import com.g42.platform.gms.notification.infrastructure.repository.ZaloTokenRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ZaloTokenAutoRefresher {
    private final ZaloTokenRepo zaloTokenRepo;
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${zalo.app-id}")
    private String appId;
    @Value("${zalo.secret-key}")
    private String secretKey;
    @Scheduled(fixedRate = 72000000)
//    @Scheduled(fixedRate = 60000)
    public void refreshZaloTokenAutomatically() {
        System.out.println("[System] Start Refreshing Zalo Token...");

        // 1. Lấy token hiện tại từ Database
        ZaloToken currentToken = zaloTokenRepo.getZaloTokenByState("active");
        if (currentToken == null || currentToken.getRefreshToken() == null) {
            System.err.println("Refresh token failed!");
            return;
        }

        // 2. Chuẩn bị gọi API đổi Token của Zalo
        String url = "https://oauth.zaloapp.com/v4/oa/access_token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); // Bắt buộc
        headers.set("secret_key", secretKey); // Nhét Secret Key vào Header

        // 3. Nối chuỗi Body dạng form-data
        String body = "app_id=" + appId +
                "&grant_type=refresh_token" +
                "&refresh_token=" + currentToken.getRefreshToken();

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            // 1. Ép RestTemplate nhận về String thuần túy thay vì Map
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            // 2. Tự dùng ObjectMapper để bóc tách chuỗi String thành JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response.getBody());

            // 3. Kiểm tra xem có trường access_token không
            if (response.getStatusCode() == HttpStatus.OK && jsonNode.has("access_token")) {


                currentToken.setAccessToken(jsonNode.get("access_token").asText());
                currentToken.setRefreshToken(jsonNode.get("refresh_token").asText());
                long expiresInSeconds = jsonNode.get("expires_in").asLong();
                currentToken.setExpiresAt(Instant.now().plusSeconds(expiresInSeconds));
                // Lưu lại vào MySQL
                zaloTokenRepo.save(currentToken);
                System.out.println("[System] Refreshing Zalo Token Successful!");
            } else {
                System.err.println("[System] Error while refreshing Zalo Token: " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("[System] Error while refreshing Zalo Token: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
