package com.g42.platform.gms.marketing.siteheader.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.marketing.siteheader.infrastructure.entity.SiteHeaderConfigJpa;
import com.g42.platform.gms.marketing.siteheader.infrastructure.repository.SiteHeaderConfigJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Nghiệp vụ bố cục thanh đầu trang khách.
 *
 * <p>Cấu trúc bên trong JSON (danh sách widget, thuộc tính từng loại) do phía
 * giao diện định nghĩa ở {@code src/services/siteHeaderService.js}. Ở đây cố ý
 * không mô tả lại từng trường: thêm một loại widget mới sẽ phải sửa cả hai nơi và
 * chỉ cần quên một chỗ là bố cục lưu được nhưng không hiện ra. Backend chỉ bảo
 * đảm phần thực sự thuộc trách nhiệm của mình — dữ liệu là JSON đúng cú pháp, là
 * một đối tượng, và không lớn quá mức.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteHeaderService {

    /** Hiện chỉ có thanh đầu trang; khớp location_code của nav_menu_item. */
    public static final String LOCATION_HEADER_MAIN = "HEADER_MAIN";

    /**
     * Trần kích thước một bố cục. Bố cục thật chỉ vài KB; đặt trần để một payload
     * hỏng hoặc cố tình bơm phồng không nằm lại trong bảng rồi đi theo mọi lượt
     * tải trang khách.
     */
    private static final int MAX_CONFIG_BYTES = 256 * 1024;

    private final SiteHeaderConfigJpaRepo configRepo;
    private final ObjectMapper objectMapper;

    /**
     * Bố cục đang lưu, hoặc {@code null} khi chưa ai cấu hình.
     *
     * <p>Trả {@code null} chứ không dựng sẵn một bố cục mặc định ở đây: giao diện
     * đã có bố cục mặc định để hiển thị khi không gọi được API, nên đặt thêm một
     * bản mặc định thứ hai ở backend là hai nguồn sự thật sẽ lệch nhau.
     */
    @Transactional(readOnly = true)
    public JsonNode getConfig(String locationCode) {
        return configRepo.findByLocationCode(locationCode)
                .map(this::readJson)
                .orElse(null);
    }

    @Transactional
    public JsonNode saveConfig(String locationCode, JsonNode config, Integer staffId) {
        if (config == null || !config.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bố cục gửi lên phải là một đối tượng JSON");
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không đọc được bố cục gửi lên", e);
        }

        if (json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_CONFIG_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Bố cục quá lớn — ảnh trong bố cục phải lưu bằng đường dẫn, không nhúng thẳng dữ liệu ảnh");
        }

        SiteHeaderConfigJpa entity = configRepo.findByLocationCode(locationCode)
                .orElseGet(() -> {
                    SiteHeaderConfigJpa created = new SiteHeaderConfigJpa();
                    created.setLocationCode(locationCode);
                    return created;
                });
        entity.setConfigJson(json);
        entity.setUpdatedBy(staffId);
        configRepo.save(entity);

        return config;
    }

    /**
     * JSON hỏng trong bảng không được phép làm chết thanh đầu trang: ghi log rồi
     * trả null để trang khách rơi về bố cục mặc định của giao diện.
     */
    private JsonNode readJson(SiteHeaderConfigJpa entity) {
        try {
            JsonNode node = objectMapper.readTree(entity.getConfigJson());
            return node != null && node.isObject() ? node : null;
        } catch (JsonProcessingException e) {
            log.error("Bố cục thanh đầu trang {} lưu trong DB không phải JSON hợp lệ", entity.getLocationCode(), e);
            return null;
        }
    }
}
