package com.g42.platform.gms.document.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.document.dto.DocumentKindDto;
import com.g42.platform.gms.document.dto.DocumentPrintSetDto;
import com.g42.platform.gms.document.entity.DocumentKind;
import com.g42.platform.gms.document.entity.DocumentPrintSet;
import com.g42.platform.gms.document.repository.DocumentKindRepository;
import com.g42.platform.gms.document.repository.DocumentPrintSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Bộ "In chứng từ" theo màn hình — màn nào in được dạng chứng từ nào.
 *
 * Giữ đúng tinh thần của phân hệ biểu mẫu: mã màn hình và danh sách mã dạng là
 * dữ liệu frontend định nghĩa, backend chỉ kiểm tra hình thức rồi lưu. Danh sách
 * được phép lẫn mã phiếu viết tay cũ (LEGACY_*) không có trong document_kind.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPrintSetService {

    /** Mã màn hình / mã dạng: chữ hoa, số, gạch dưới — giống document_kind.code. */
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9_]{1,50}$");
    private static final int MAX_KINDS = 20;

    private final DocumentPrintSetRepository printSetRepository;
    private final DocumentKindRepository kindRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<DocumentPrintSetDto> listAll() {
        Map<String, DocumentKind> kindsByCode = kindsByCode();
        return printSetRepository.findAll().stream()
                .sorted(Comparator.comparing(DocumentPrintSet::getScreenCode))
                .map(set -> toDto(set, kindsByCode))
                .toList();
    }

    /**
     * Bộ in của một màn. Chưa chỉnh lần nào thì trả về {@code customized = false}
     * kèm danh sách rỗng — frontend tự dùng bộ mặc định trong code.
     */
    @Transactional(readOnly = true)
    public DocumentPrintSetDto get(String screenCode) {
        String code = requireCode(screenCode, "Mã màn hình");
        Map<String, DocumentKind> kindsByCode = kindsByCode();
        return printSetRepository.findById(code)
                .map(set -> toDto(set, kindsByCode))
                .orElseGet(() -> DocumentPrintSetDto.builder()
                        .screenCode(code)
                        .kindCodes(List.of())
                        .customized(false)
                        .kinds(List.of())
                        .build());
    }

    @Transactional
    public DocumentPrintSetDto save(String screenCode, DocumentPrintSetDto dto, Integer staffId) {
        String code = requireCode(screenCode, "Mã màn hình");

        // Bỏ trùng, giữ thứ tự nhân viên đã sắp.
        LinkedHashSet<String> kindCodes = new LinkedHashSet<>();
        for (String raw : Optional.ofNullable(dto.getKindCodes()).orElse(List.of())) {
            kindCodes.add(requireCode(raw, "Mã dạng chứng từ"));
        }
        if (kindCodes.isEmpty()) {
            throw new IllegalArgumentException("Bộ in phải có ít nhất một dạng chứng từ");
        }
        if (kindCodes.size() > MAX_KINDS) {
            throw new IllegalArgumentException("Một màn chỉ nên có tối đa " + MAX_KINDS + " dạng chứng từ");
        }

        String defaultKind = dto.getDefaultKindCode() == null || dto.getDefaultKindCode().isBlank()
                ? null
                : requireCode(dto.getDefaultKindCode(), "Dạng chọn sẵn");
        if (defaultKind != null && !kindCodes.contains(defaultKind)) {
            throw new IllegalArgumentException("Dạng chọn sẵn phải nằm trong bộ in");
        }

        DocumentPrintSet set = printSetRepository.findById(code).orElseGet(() -> {
            DocumentPrintSet fresh = new DocumentPrintSet();
            fresh.setScreenCode(code);
            return fresh;
        });
        set.setKindCodes(writeCodes(new ArrayList<>(kindCodes)));
        set.setDefaultKindCode(defaultKind);
        set.setUpdatedBy(staffId);
        return toDto(printSetRepository.save(set), kindsByCode());
    }

    /** Bỏ cấu hình riêng — màn đó quay về bộ mặc định trong code. */
    @Transactional
    public void reset(String screenCode) {
        String code = requireCode(screenCode, "Mã màn hình");
        printSetRepository.findById(code).ifPresent(printSetRepository::delete);
    }

    // ---------------------------------------------------------------- nội bộ

    private Map<String, DocumentKind> kindsByCode() {
        Map<String, DocumentKind> map = new HashMap<>();
        for (DocumentKind kind : kindRepository.findAll()) {
            map.put(kind.getCode().toUpperCase(Locale.ROOT), kind);
        }
        return map;
    }

    private DocumentPrintSetDto toDto(DocumentPrintSet set, Map<String, DocumentKind> kindsByCode) {
        List<String> codes = readCodes(set.getKindCodes());
        List<DocumentKindDto> kinds = codes.stream()
                .map(kindsByCode::get)
                .filter(Objects::nonNull)
                .map(kind -> DocumentKindDto.builder()
                        .kindId(kind.getKindId())
                        .code(kind.getCode())
                        .name(kind.getName())
                        .dataSource(kind.getDataSource() == null ? null : kind.getDataSource().name())
                        .stage(kind.getStage() == null ? null : kind.getStage().name())
                        .docNoPattern(kind.getDocNoPattern())
                        .active(Boolean.TRUE.equals(kind.getActive()))
                        .system(Boolean.TRUE.equals(kind.getSystem()))
                        .build())
                .toList();
        return DocumentPrintSetDto.builder()
                .screenCode(set.getScreenCode())
                .kindCodes(codes)
                .defaultKindCode(set.getDefaultKindCode())
                .customized(true)
                .updatedAt(set.getUpdatedAt())
                .kinds(kinds)
                .build();
    }

    private List<String> readCodes(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception e) {
            log.warn("Bộ in chứng từ lưu sai định dạng, coi như rỗng: {}", e.getMessage());
            return List.of();
        }
    }

    private String writeCodes(List<String> codes) {
        try {
            return objectMapper.writeValueAsString(codes);
        } catch (Exception e) {
            throw new IllegalStateException("Không ghi được bộ in chứng từ", e);
        }
    }

    private static String requireCode(String raw, String label) {
        String code = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(code).matches()) {
            throw new IllegalArgumentException(label + " không hợp lệ: " + raw);
        }
        return code;
    }
}
