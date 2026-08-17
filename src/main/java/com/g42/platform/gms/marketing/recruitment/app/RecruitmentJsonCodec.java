package com.g42.platform.gms.marketing.recruitment.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.marketing.news.app.SlugGenerator;
import com.g42.platform.gms.marketing.recruitment.api.dto.RecruitmentDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Đọc/ghi hai chỗ dữ liệu lưu dạng JSON của phân hệ tuyển dụng: danh sách câu
 * hỏi thêm của form và câu trả lời tương ứng.
 *
 * <p>Mọi lỗi đọc đều trả về giá trị rỗng chứ không ném ngoại lệ: một tin tuyển
 * dụng có JSON hỏng vẫn phải mở ra xem được, chỉ mất phần câu hỏi thêm — hỏng
 * dữ liệu phụ không đáng để cả trang trả về lỗi 500.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitmentJsonCodec {

    /** Giới hạn số câu hỏi thêm, chặn việc lưu một chuỗi JSON khổng lồ. */
    private static final int MAX_FIELDS = 30;

    private static final List<String> ALLOWED_TYPES =
            List.of("TEXT", "TEXTAREA", "NUMBER", "SELECT", "CHECKBOX", "DATE");

    private final ObjectMapper objectMapper;

    // ------------------------------------------------------ câu hỏi thêm

    public List<RecruitmentDtos.FormFieldDto> readFields(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<RecruitmentDtos.FormFieldDto> parsed =
                    objectMapper.readValue(json, new TypeReference<List<RecruitmentDtos.FormFieldDto>>() {
                    });
            return parsed == null ? List.of() : parsed;
        } catch (Exception e) {
            log.warn("Tuyển dụng: form_fields_json hỏng, bỏ qua phần câu hỏi thêm: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Chuẩn hoá rồi ghi thành JSON.
     *
     * <p>Khoá của câu hỏi được sinh từ nhãn nếu người dùng không tự đặt, và
     * luôn được làm cho duy nhất — trùng khoá thì câu trả lời sau ghi đè câu
     * trước trong {@code answers_json} và mất hẳn dữ liệu ứng viên gửi lên.
     */
    public String writeFields(List<RecruitmentDtos.FormFieldDto> fields) {
        if (fields == null || fields.isEmpty()) return null;

        List<RecruitmentDtos.FormFieldDto> normalized = new ArrayList<>();
        List<String> usedKeys = new ArrayList<>();

        for (RecruitmentDtos.FormFieldDto field : fields) {
            if (field == null || field.label() == null || field.label().isBlank()) continue;
            if (normalized.size() >= MAX_FIELDS) break;

            String key = uniqueKey(field.key(), field.label(), usedKeys);
            usedKeys.add(key);

            String type = field.type() == null ? "TEXT" : field.type().trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_TYPES.contains(type)) type = "TEXT";

            List<String> options = field.options() == null ? List.of() : field.options().stream()
                    .filter(option -> option != null && !option.isBlank())
                    .map(String::trim)
                    .limit(30)
                    .toList();

            normalized.add(new RecruitmentDtos.FormFieldDto(
                    key,
                    field.label().trim(),
                    type,
                    Boolean.TRUE.equals(field.required()),
                    field.placeholder(),
                    options));
        }

        if (normalized.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            log.error("Tuyển dụng: không tuần tự hoá được câu hỏi thêm", e);
            return null;
        }
    }

    private String uniqueKey(String rawKey, String label, List<String> used) {
        String base = slugKey(rawKey != null && !rawKey.isBlank() ? rawKey : label);
        if (base.isEmpty()) base = "cau-hoi";
        if (!used.contains(base)) return base;
        for (int suffix = 2; suffix < 100; suffix++) {
            String candidate = base + "-" + suffix;
            if (!used.contains(candidate)) return candidate;
        }
        return base + "-" + System.nanoTime();
    }

    /** Dùng chung bộ sinh slug của phân hệ tin tức thay vì chép lại luật bỏ dấu tiếng Việt. */
    private String slugKey(String raw) {
        String slug = SlugGenerator.toSlug(raw);
        return slug.length() > 60 ? slug.substring(0, 60).replaceAll("-+$", "") : slug;
    }

    // ---------------------------------------------------------- câu trả lời

    public Map<String, String> readAnswers(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            Map<String, String> parsed =
                    objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {
                    });
            return parsed == null ? Map.of() : parsed;
        } catch (Exception e) {
            log.warn("Tuyển dụng: answers_json hỏng ở một hồ sơ: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * Chỉ giữ lại câu trả lời khớp với câu hỏi mà tin tuyển dụng thực sự khai
     * báo — người gửi tự dựng được thân yêu cầu, nên không lọc thì bảng hồ sơ
     * biến thành nơi chứa dữ liệu tuỳ ý của người lạ.
     */
    public String writeAnswers(Map<String, String> answers, List<RecruitmentDtos.FormFieldDto> declaredFields) {
        if (answers == null || answers.isEmpty() || declaredFields == null || declaredFields.isEmpty()) return null;

        List<String> allowedKeys = declaredFields.stream().map(RecruitmentDtos.FormFieldDto::key).toList();
        Map<String, String> filtered = new LinkedHashMap<>();
        answers.forEach((key, value) -> {
            if (key == null || !allowedKeys.contains(key)) return;
            if (value == null || value.isBlank()) return;
            filtered.put(key, value.length() > 1000 ? value.substring(0, 1000) : value);
        });

        if (filtered.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(filtered);
        } catch (Exception e) {
            log.error("Tuyển dụng: không tuần tự hoá được câu trả lời", e);
            return null;
        }
    }
}
