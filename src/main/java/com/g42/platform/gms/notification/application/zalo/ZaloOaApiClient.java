package com.g42.platform.gms.notification.application.zalo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Các API chỉ-đọc của Zalo dùng cho màn /zalo-oa-config.
 *
 * Trả JsonNode ("data" trong phản hồi Zalo) nguyên vẹn cho FE tự đọc trường: tên trường của
 * Zalo đổi giữa các phiên bản API (snake_case ở OA, camelCase ở ZNS) và FE đã đọc có dự phòng,
 * nên BE không ép vào DTO cứng để khỏi vỡ khi Zalo thêm/bớt trường.
 *
 * Lỗi phía Zalo (error != 0) -> 400 kèm thông báo + mã lỗi của Zalo, để màn cấu hình hiện
 * đúng nguyên nhân (token hết hạn -216/-124, OA chưa duyệt ZNS...).
 */
@Service
@RequiredArgsConstructor
public class ZaloOaApiClient {

    private static final String OA_INFO_URL = "https://openapi.zalo.me/v2.0/oa/getoa";
    private static final String ZNS_BASE = "https://business.openapi.zalo.me";

    private final ZaloTokenService tokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    /** Thông tin OA — cũng là cách kiểm tra token còn sống hay không. */
    public JsonNode getOaInfo() {
        return get(OA_INFO_URL);
    }

    /** Hạn mức gửi ZNS trong ngày. */
    public JsonNode getZnsQuota() {
        return get(ZNS_BASE + "/message/quota");
    }

    /** Danh sách mẫu ZNS của OA. status: 1 = đã duyệt, 2 = chờ duyệt, 3 = bị từ chối, 4 = tạm khoá; null = tất cả. */
    public JsonNode listTemplates(int offset, int limit, Integer status) {
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(ZNS_BASE + "/template/all")
                .queryParam("offset", Math.max(0, offset))
                .queryParam("limit", Math.min(100, Math.max(1, limit)));
        if (status != null) b.queryParam("status", status);
        return get(b.toUriString());
    }

    /** Chi tiết 1 mẫu: danh sách tham số, giá, link xem trước. */
    public JsonNode templateInfo(String templateId) {
        if (templateId == null || !templateId.matches("\\d{1,20}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã mẫu không hợp lệ.");
        }
        return get(ZNS_BASE + "/template/info/v2?template_id=" + templateId);
    }

    /** Trạng thái 1 tin ZNS đã gửi (theo msg_id). */
    public JsonNode messageStatus(String msgId) {
        if (msgId == null || !msgId.matches("[A-Za-z0-9_-]{1,100}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "msg_id không hợp lệ.");
        }
        return get(ZNS_BASE + "/message/status?message_id=" + msgId);
    }

    private JsonNode get(String url) {
        String token = tokenService.currentAccessToken();
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa có access token Zalo OA.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("access_token", token);
        String body;
        try {
            ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            body = res.getBody();
        } catch (RestClientResponseException e) {
            body = e.getResponseBodyAsString();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không gọi được máy chủ Zalo: " + e.getMessage());
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(body == null ? "{}" : body);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Phản hồi của Zalo không phải JSON.");
        }
        int error = root.path("error").asInt(0);
        if (error != 0) {
            String msg = root.path("message").asText("Zalo báo lỗi");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg + " (mã lỗi Zalo " + error + ")");
        }
        // ZNS danh sách mẫu đặt tổng số ở "metadata" ngang hàng với "data" — giữ cả gói.
        if (root.has("metadata")) return root;
        return root.has("data") ? root.get("data") : root;
    }
}
