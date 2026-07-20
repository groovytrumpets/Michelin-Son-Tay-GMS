package com.g42.platform.gms.push.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.dashboard.domain.entity.StaffNotification;
import com.g42.platform.gms.push.infrastructure.entity.PushSubscriptionJpa;
import com.g42.platform.gms.push.infrastructure.repository.PushSubscriptionRepository;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gửi Web Push (thông báo cấp hệ điều hành) cho các subscription của nhân viên.
 * Chạy bất đồng bộ để không chặn luồng tạo/gửi thông báo (STOMP). Nếu push service
 * trả 404/410 (subscription hết hạn) thì tự set active=false.
 *
 * Lưu ý: thư viện nl.martijndwars web-push mặc định gửi qua Apache HttpAsyncClient
 * (NIO) cũ không gửi SNI -> handshake TLS tới fcm.googleapis.com trả về certificate
 * sai host (CN=upload.video.google.com). Vì vậy ở đây ta chỉ dùng thư viện để
 * MÃ HOÁ + dựng request (preparePost), còn GỬI bằng java.net.http.HttpClient của JDK
 * (SNI đúng).
 */
@Service
public class WebPushDispatchService {

    private static final Logger log = LoggerFactory.getLogger(WebPushDispatchService.class);

    private final ObjectProvider<PushService> pushServiceProvider;
    private final PushSubscriptionRepository repository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WebPushDispatchService(ObjectProvider<PushService> pushServiceProvider,
                                  PushSubscriptionRepository repository,
                                  ObjectMapper objectMapper) {
        this.pushServiceProvider = pushServiceProvider;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Gửi push cho 1 thông báo đã lưu. staffId == null nghĩa là broadcast toàn bộ
     * nhân viên -> gửi mọi subscription đang active.
     */
    @Async
    public void dispatch(StaffNotification notification) {
        PushService pushService = pushServiceProvider.getIfAvailable();
        if (pushService == null) {
            // Chưa cấu hình VAPID -> bỏ qua, chỉ dựa vào STOMP khi web đang mở.
            return;
        }
        if (notification == null) return;

        List<PushSubscriptionJpa> targets = notification.getStaffId() == null
                ? repository.findByActiveTrue()
                : repository.findByStaffIdAndActiveTrue(notification.getStaffId());

        Map<String, Object> body = new HashMap<>();
        body.put("title", notification.getTitle() != null ? notification.getTitle() : "Thông báo mới");
        body.put("body", notification.getMessage());
        body.put("url", notification.getUrl());
        body.put("type", notification.getNotificationType() != null ? notification.getNotificationType().name() : "INFO");
        body.put("notificationId", notification.getNotificationId());
        if (notification.getNotificationId() != null) {
            body.put("tag", "noti-" + notification.getNotificationId());
        }
        pushToSubscriptions(pushService, targets, toBytes(body));
    }

    /**
     * Gửi push cho tin nhắn chat tới các nhân viên nhận (trừ người gửi). URL mở app
     * kèm ?openChat=<conversationId> để FE tự bung đúng cửa sổ hội thoại.
     */
    @Async
    public void sendChatMessage(List<Integer> recipientStaffIds, String senderName,
                                String preview, Integer conversationId) {
        PushService pushService = pushServiceProvider.getIfAvailable();
        if (pushService == null) return;
        if (recipientStaffIds == null || recipientStaffIds.isEmpty()) return;

        Map<String, Object> body = new HashMap<>();
        body.put("title", senderName != null && !senderName.isBlank() ? senderName : "Tin nhắn mới");
        body.put("body", preview);
        body.put("url", conversationId != null ? "/dashboard?openChat=" + conversationId : "/dashboard");
        body.put("type", "CHAT");
        if (conversationId != null) {
            // Cùng conversation gộp chung 1 tag -> thông báo mới thay thế thông báo cũ.
            body.put("tag", "chat-" + conversationId);
        }
        byte[] payload = toBytes(body);

        for (Integer staffId : recipientStaffIds) {
            if (staffId == null) continue;
            pushToSubscriptions(pushService, repository.findByStaffIdAndActiveTrue(staffId), payload);
        }
    }

    private void pushToSubscriptions(PushService pushService, List<PushSubscriptionJpa> targets, byte[] payload) {
        if (targets == null || targets.isEmpty()) return;
        for (PushSubscriptionJpa sub : targets) {
            sendOne(pushService, sub, payload);
        }
    }

    private byte[] toBytes(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            log.warn("Không tạo được payload push: {}", e.getMessage());
            return "{}".getBytes(StandardCharsets.UTF_8);
        }
    }

    private void sendOne(PushService pushService, PushSubscriptionJpa sub, byte[] payload) {
        try {
            Notification notification = new Notification(
                    sub.getEndpoint(),
                    sub.getP256dh(),
                    sub.getAuth(),
                    payload);

            // Dùng thư viện để mã hoá (aes128gcm) + ký VAPID và dựng request...
            HttpPost apachePost = pushService.preparePost(notification, Encoding.AES128GCM);
            byte[] encryptedBody = apachePost.getEntity() != null
                    ? EntityUtils.toByteArray(apachePost.getEntity())
                    : new byte[0];

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(apachePost.getURI().toString()))
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(encryptedBody));

            for (Header h : apachePost.getAllHeaders()) {
                builder.header(h.getName(), h.getValue());
            }

            // ...nhưng GỬI bằng JDK HttpClient để TLS gửi SNI đúng.
            HttpResponse<Void> response = httpClient.send(
                    builder.build(), HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();

            // 404/410: endpoint không còn tồn tại -> dọn subscription.
            if (status == 404 || status == 410) {
                repository.deactivateByEndpoint(sub.getEndpoint());
                log.info("Đã vô hiệu hoá push subscription hết hạn (status {}) endpoint={}",
                        status, sub.getEndpoint());
            } else if (status >= 400) {
                log.warn("Gửi push thất bại status {} endpoint={}", status, sub.getEndpoint());
            }
        } catch (Exception e) {
            // Không để lỗi push làm ảnh hưởng luồng thông báo chính.
            log.warn("Lỗi gửi push tới endpoint={}: {}", sub.getEndpoint(), e.getMessage());
        }
    }
}
