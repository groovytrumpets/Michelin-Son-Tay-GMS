package com.g42.platform.gms.push.config;

import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.EnableAsync;

import java.security.GeneralSecurityException;
import java.security.Security;

/**
 * Cấu hình Web Push (VAPID). Bean {@link PushService} chỉ được tạo khi có đủ khoá
 * vapid.public-key / vapid.private-key trong cấu hình — thiếu khoá thì tính năng tự
 * tắt (dispatcher no-op) để không chặn khởi động.
 *
 * Sinh cặp khoá VAPID: `npx web-push generate-vapid-keys` (Public Key dán vào FE
 * VITE_VAPID_PUBLIC_KEY, cả hai dán vào cấu hình backend bên dưới).
 */
@Configuration
@EnableAsync
public class WebPushConfig {

    private static final Logger log = LoggerFactory.getLogger(WebPushConfig.class);

    @Value("${vapid.public-key:}")
    private String publicKey;

    @Value("${vapid.private-key:}")
    private String privateKey;

    @Value("${vapid.subject:mailto:admin@sontaygarage.vn}")
    private String subject;

    @PostConstruct
    void registerBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Trả về null (Web Push tắt) khi chưa cấu hình khoá VAPID — WebPushDispatchService
     * inject qua ObjectProvider nên no-op an toàn. Chỉ khởi tạo PushService khi có đủ
     * cả public/private key, tránh BouncyCastle decode khoá rỗng.
     */
    @Bean
    @Nullable
    public PushService pushService() {
        if (publicKey == null || publicKey.isBlank() || privateKey == null || privateKey.isBlank()) {
            log.warn("Web Push chưa bật: thiếu vapid.public-key/vapid.private-key. "
                    + "Sinh khoá bằng `npx web-push generate-vapid-keys` rồi set env VAPID_PUBLIC_KEY/VAPID_PRIVATE_KEY.");
            return null;
        }
        
        try {
            PushService service = new PushService();
            service.setPublicKey(publicKey);
            service.setPrivateKey(privateKey);
            service.setSubject(subject);
            log.info("Web Push đã bật (VAPID).");
            return service;
        } catch (Exception e) {
            log.error("Khoá VAPID không hợp lệ (lỗi format). Web Push sẽ tự động tắt. Lỗi: {}", e.getMessage());
            return null;
        }
    }
}
