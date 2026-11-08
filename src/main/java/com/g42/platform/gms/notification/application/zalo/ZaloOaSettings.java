package com.g42.platform.gms.notification.application.zalo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.notification.domain.ZnsTemplateType;
import com.g42.platform.gms.notification.infrastructure.entity.ZaloOaConfig;
import com.g42.platform.gms.notification.infrastructure.repository.ZaloOaConfigRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nguồn duy nhất cho App ID / Secret key / Redirect URI / mã mẫu ZNS.
 *
 * Ưu tiên giá trị lưu ở bảng zalo_oa_config (sửa được trên /zalo-oa-config); ô nào trống thì
 * dùng application.properties. Nhờ vậy DB mới chạy Liquibase 053 xong mà chưa ai lưu gì thì
 * hệ thống vẫn chạy y như trước.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZaloOaSettings {

    private final ZaloOaConfigRepo configRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${zalo.app-id:}")
    private String propertyAppId;

    @Value("${zalo.secret-key:}")
    private String propertySecretKey;

    /** Phải khớp đúng Callback URL khai báo trong ứng dụng ở developers.zalo.me. */
    @Value("${zalo.redirect-uri:https://sontaygarage.vn/zalo/callback}")
    private String propertyRedirectUri;

    public String appId() {
        return firstNonBlank(row().getAppId(), propertyAppId);
    }

    public String secretKey() {
        return firstNonBlank(row().getSecretKey(), propertySecretKey);
    }

    public String redirectUri() {
        return firstNonBlank(row().getRedirectUri(), propertyRedirectUri);
    }

    public boolean appIdFromDb() { return notBlank(row().getAppId()); }
    public boolean secretKeyFromDb() { return notBlank(row().getSecretKey()); }
    public boolean redirectUriFromDb() { return notBlank(row().getRedirectUri()); }

    public String templateId(ZnsTemplateType type) {
        String custom = customTemplateMap().get(type.name());
        return notBlank(custom) ? custom.trim() : type.getDefaultTemplateId();
    }

    /** Mã mẫu đang dùng cho từng loại tin (đã gộp giá trị mặc định). */
    public Map<ZnsTemplateType, String> effectiveTemplateMap() {
        Map<ZnsTemplateType, String> map = new EnumMap<>(ZnsTemplateType.class);
        for (ZnsTemplateType type : ZnsTemplateType.values()) {
            map.put(type, templateId(type));
        }
        return map;
    }

    public Map<String, String> customTemplateMap() {
        String json = row().getTemplateMap();
        if (!notBlank(json)) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (Exception e) {
            log.warn("Zalo: template_map trong zalo_oa_config hỏng, dùng mã mặc định: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * Lưu cấu hình. Với từng ô: null = giữ nguyên, chuỗi rỗng = xoá (quay về application.properties).
     * Secret key không bao giờ được trả về FE nên FE gửi null khi người dùng không gõ gì.
     */
    @Transactional
    public void update(String appId, String secretKey, String redirectUri,
                       Map<String, String> templateMap, Integer staffId) {
        ZaloOaConfig cfg = configRepo.findById(ZaloOaConfig.SINGLETON_ID).orElseGet(() -> {
            ZaloOaConfig c = new ZaloOaConfig();
            c.setId(ZaloOaConfig.SINGLETON_ID);
            return c;
        });
        if (appId != null) {
            String v = appId.trim();
            if (!v.isEmpty() && !v.matches("\\d{5,30}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "App ID của Zalo chỉ gồm chữ số.");
            }
            cfg.setAppId(v.isEmpty() ? null : v);
        }
        if (secretKey != null) {
            String v = secretKey.trim();
            cfg.setSecretKey(v.isEmpty() ? null : v);
        }
        if (redirectUri != null) {
            String v = redirectUri.trim();
            if (!v.isEmpty() && !v.startsWith("https://") && !v.startsWith("http://localhost")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Redirect URI phải bắt đầu bằng https:// (Zalo không chấp nhận http).");
            }
            cfg.setRedirectUri(v.isEmpty() ? null : v);
        }
        if (templateMap != null) {
            Map<String, String> clean = new LinkedHashMap<>();
            for (ZnsTemplateType type : ZnsTemplateType.values()) {
                String v = templateMap.get(type.name());
                if (v == null || v.isBlank()) continue;
                v = v.trim();
                if (!v.matches("\\d{1,20}")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Mã mẫu ZNS cho \"" + type.getLabel() + "\" phải là số.");
                }
                if (!v.equals(type.getDefaultTemplateId())) clean.put(type.name(), v);
            }
            try {
                cfg.setTemplateMap(clean.isEmpty() ? null : objectMapper.writeValueAsString(clean));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        cfg.setUpdatedAt(Instant.now());
        cfg.setUpdatedByStaffId(staffId);
        configRepo.save(cfg);
    }

    public ZaloOaConfig row() {
        return configRepo.findById(ZaloOaConfig.SINGLETON_ID).orElseGet(ZaloOaConfig::new);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String firstNonBlank(String a, String b) {
        return notBlank(a) ? a.trim() : (b == null ? "" : b.trim());
    }
}
