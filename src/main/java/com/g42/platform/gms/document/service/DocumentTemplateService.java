package com.g42.platform.gms.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.document.dto.CompanyProfileDto;
import com.g42.platform.gms.document.dto.DocumentKindDto;
import com.g42.platform.gms.document.dto.DocumentTemplateDto;
import com.g42.platform.gms.document.dto.DocumentTemplateSummaryDto;
import com.g42.platform.gms.document.entity.*;
import com.g42.platform.gms.document.repository.CompanyProfileRepository;
import com.g42.platform.gms.document.repository.DocumentKindRepository;
import com.g42.platform.gms.document.repository.DocumentTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Biểu mẫu chứng từ: dạng chứng từ, bố cục mẫu và hồ sơ công ty.
 *
 * Nguyên tắc xuyên suốt: backend KHÔNG hiểu nội dung bố cục. {@code layoutJson}
 * là Template JSON của pdfme, ở đây chỉ được lưu và trả lại nguyên vẹn, ngoài
 * việc đếm vài con số để hiển thị. Nhờ vậy thêm loại ô mới cho trình thiết kế
 * chỉ cần sửa frontend, không phải chạy migration hay build lại backend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentTemplateService {

    private final DocumentKindRepository kindRepository;
    private final DocumentTemplateRepository templateRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------------------------------------------------------------- dạng chứng từ

    @Transactional(readOnly = true)
    public List<DocumentKindDto> listKinds(boolean activeOnly) {
        List<DocumentKind> kinds = activeOnly
                ? kindRepository.findByActiveTrueOrderBySortOrderAscNameAsc()
                : kindRepository.findAllByOrderBySortOrderAscNameAsc();

        return kinds.stream().map(kind -> {
            List<DocumentTemplate> templates = templateRepository
                    .findByKindIdOrderByDefaultTemplateDescNameAsc(kind.getKindId());
            String defaultName = templates.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getDefaultTemplate()) && Boolean.TRUE.equals(t.getActive()))
                    .map(DocumentTemplate::getName)
                    .findFirst()
                    .orElse(null);
            return toKindDto(kind, templates.size(), defaultName);
        }).toList();
    }

    /**
     * Dạng chứng từ hợp lệ tại một điểm trong luồng nghiệp vụ — dùng để đổ vào ô
     * chọn "In chứng từ" ở các màn bán hàng, sửa xe, kho. Bỏ trống {@code stage}
     * thì trả về mọi giai đoạn của nguồn dữ liệu đó.
     */
    @Transactional(readOnly = true)
    public List<DocumentKindDto> listKindsFor(String dataSource, String stage) {
        DataSourceType source = parseEnum(DataSourceType.class, dataSource, "nguồn dữ liệu");
        List<DocumentKind> kinds = (stage == null || stage.isBlank())
                ? kindRepository.findByActiveTrueAndDataSourceOrderBySortOrderAscNameAsc(source)
                : kindRepository.findByActiveTrueAndDataSourceAndStageOrderBySortOrderAscNameAsc(
                        source, parseEnum(DocumentStage.class, stage, "giai đoạn"));

        // Chỉ trả về dạng đã có ít nhất một mẫu dùng được — dạng chưa dựng mẫu mà
        // hiện ra ô chọn thì nhân viên bấm vào sẽ ra tờ giấy trắng.
        return kinds.stream()
                .map(kind -> {
                    List<DocumentTemplate> usable = templateRepository
                            .findByKindIdAndActiveTrueOrderByDefaultTemplateDescNameAsc(kind.getKindId());
                    if (usable.isEmpty()) return null;
                    return toKindDto(kind, usable.size(), usable.get(0).getName());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public DocumentKindDto createKind(DocumentKindDto dto) {
        String code = normalizeCode(dto.getCode());
        if (code.isEmpty()) {
            throw new IllegalArgumentException("Mã dạng chứng từ không được để trống");
        }
        if (kindRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Mã dạng chứng từ '" + code + "' đã tồn tại");
        }
        requireText(dto.getName(), "Tên dạng chứng từ");

        DocumentKind kind = new DocumentKind();
        kind.setCode(code);
        kind.setSystem(false);   // dạng do người dùng tạo thì luôn xoá được
        applyKind(kind, dto);
        return toKindDto(kindRepository.save(kind), 0, null);
    }

    @Transactional
    public DocumentKindDto updateKind(Integer kindId, DocumentKindDto dto) {
        DocumentKind kind = kindRepository.findById(kindId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dạng chứng từ"));
        requireText(dto.getName(), "Tên dạng chứng từ");

        // Mã của dạng hệ thống là thứ code trỏ tới, đổi là gãy chỗ gọi.
        if (!Boolean.TRUE.equals(kind.getSystem())) {
            String code = normalizeCode(dto.getCode());
            if (!code.isEmpty() && !code.equalsIgnoreCase(kind.getCode())) {
                if (kindRepository.existsByCodeIgnoreCase(code)) {
                    throw new IllegalArgumentException("Mã dạng chứng từ '" + code + "' đã tồn tại");
                }
                kind.setCode(code);
            }
        }

        applyKind(kind, dto);
        long templateCount = templateRepository.countByKindId(kindId);
        return toKindDto(kindRepository.save(kind), (int) templateCount, null);
    }

    @Transactional
    public void deleteKind(Integer kindId) {
        DocumentKind kind = kindRepository.findById(kindId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dạng chứng từ"));
        if (Boolean.TRUE.equals(kind.getSystem())) {
            throw new IllegalArgumentException(
                    "Không xoá được dạng chứng từ của hệ thống. Nếu không dùng nữa thì bỏ đánh dấu 'Đang dùng'.");
        }
        // Mẫu thuộc dạng này bị xoá theo, nhờ khoá ngoại ON DELETE CASCADE.
        kindRepository.delete(kind);
    }

    // ---------------------------------------------------------------- mẫu

    @Transactional(readOnly = true)
    public List<DocumentTemplateSummaryDto> listTemplates(Integer kindId) {
        return templateRepository.findByKindIdOrderByDefaultTemplateDescNameAsc(kindId)
                .stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public DocumentTemplateDto getTemplate(Integer templateId) {
        DocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mẫu chứng từ"));
        DocumentKind kind = kindRepository.findById(template.getKindId()).orElse(null);
        return toTemplateDto(template, kind);
    }

    /**
     * Mẫu đang dùng của một dạng, ưu tiên mẫu mặc định. Các màn nghiệp vụ gọi
     * cái này để lấy bố cục rồi tự dựng bản in phía trình duyệt.
     */
    @Transactional(readOnly = true)
    public DocumentTemplateDto getDefaultTemplate(String kindCode) {
        DocumentKind kind = kindRepository.findByCodeIgnoreCase(normalizeCode(kindCode))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dạng chứng từ '" + kindCode + "'"));
        List<DocumentTemplate> templates = templateRepository
                .findByKindIdAndActiveTrueOrderByDefaultTemplateDescNameAsc(kind.getKindId());
        if (templates.isEmpty()) {
            throw new IllegalArgumentException("Dạng chứng từ '" + kind.getName() + "' chưa có mẫu nào đang dùng");
        }
        return toTemplateDto(templates.get(0), kind);
    }

    @Transactional
    public DocumentTemplateDto saveTemplate(Integer templateId, DocumentTemplateDto dto, Integer staffId) {
        DocumentTemplate template;
        if (templateId == null) {
            if (dto.getKindId() == null) {
                throw new IllegalArgumentException("Thiếu dạng chứng từ cho mẫu mới");
            }
            template = new DocumentTemplate();
            template.setKindId(dto.getKindId());
            template.setCreatedBy(staffId);
            template.setVersion(1);
        } else {
            template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mẫu chứng từ"));
            // Chỉ tăng version khi bố cục thật sự đổi — đổi mỗi cái tên thì không.
            if (dto.getLayoutJson() != null && !dto.getLayoutJson().equals(template.getLayoutJson())) {
                template.setVersion(Optional.ofNullable(template.getVersion()).orElse(1) + 1);
            }
        }

        requireText(dto.getName(), "Tên mẫu");
        template.setName(dto.getName().trim());
        template.setNote(trimToNull(dto.getNote()));
        template.setSourceFileName(trimToNull(dto.getSourceFileName()));
        template.setPaperSize(Optional.ofNullable(trimToNull(dto.getPaperSize())).orElse("A4"));
        if (dto.getActive() != null) template.setActive(dto.getActive());
        if (dto.getLayoutJson() != null) {
            template.setLayoutJson(dto.getLayoutJson());
            applyLayoutStats(template, dto.getLayoutJson());
        }

        DocumentKind kind = kindRepository.findById(template.getKindId())
                .orElseThrow(() -> new IllegalArgumentException("Dạng chứng từ không tồn tại"));

        DocumentTemplate saved = templateRepository.save(template);

        // Mẫu đầu tiên của một dạng tự thành mặc định, nếu không thì dạng đó có mẫu
        // mà màn nghiệp vụ vẫn báo "chưa có mẫu mặc định".
        boolean wantDefault = Boolean.TRUE.equals(dto.getDefaultTemplate());
        boolean isOnlyTemplate = templateRepository.countByKindId(saved.getKindId()) == 1;
        if (wantDefault || isOnlyTemplate) {
            markDefault(saved);
        }

        return toTemplateDto(saved, kind);
    }

    @Transactional
    public DocumentTemplateDto duplicateTemplate(Integer templateId, String newName, Integer staffId) {
        DocumentTemplate source = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mẫu chứng từ"));

        DocumentTemplate copy = new DocumentTemplate();
        copy.setKindId(source.getKindId());
        copy.setName(Optional.ofNullable(trimToNull(newName)).orElse(source.getName() + " (bản sao)"));
        copy.setLayoutJson(source.getLayoutJson());
        copy.setSourceFileName(source.getSourceFileName());
        copy.setPaperSize(source.getPaperSize());
        copy.setNote(source.getNote());
        copy.setPageCount(source.getPageCount());
        copy.setFieldCount(source.getFieldCount());
        copy.setHasBasePdf(source.getHasBasePdf());
        copy.setActive(true);
        copy.setVersion(1);
        copy.setCreatedBy(staffId);
        // Bản sao KHÔNG kế thừa cờ mặc định — nhân bản để thử sửa, chưa phải để dùng.
        copy.setDefaultTemplate(false);

        DocumentTemplate saved = templateRepository.save(copy);
        return toTemplateDto(saved, kindRepository.findById(saved.getKindId()).orElse(null));
    }

    @Transactional
    public void setDefaultTemplate(Integer templateId) {
        DocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mẫu chứng từ"));
        if (!Boolean.TRUE.equals(template.getActive())) {
            throw new IllegalArgumentException("Mẫu đang tắt thì không đặt làm mặc định được");
        }
        markDefault(template);
    }

    @Transactional
    public void deleteTemplate(Integer templateId) {
        DocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mẫu chứng từ"));
        Integer kindId = template.getKindId();
        boolean wasDefault = Boolean.TRUE.equals(template.getDefaultTemplate());
        templateRepository.delete(template);
        templateRepository.flush();

        // Xoá mất mẫu mặc định thì đôn mẫu còn lại lên, tránh để dạng đó rơi vào
        // trạng thái có mẫu nhưng không mẫu nào được chọn sẵn.
        if (wasDefault) {
            templateRepository.findByKindIdAndActiveTrueOrderByDefaultTemplateDescNameAsc(kindId)
                    .stream().findFirst().ifPresent(this::markDefault);
        }
    }

    // ---------------------------------------------------------------- hồ sơ công ty

    @Transactional(readOnly = true)
    public CompanyProfileDto getCompanyProfile() {
        return companyProfileRepository.findById(CompanyProfile.SINGLETON_ID)
                .map(this::toCompanyDto)
                .orElseGet(() -> CompanyProfileDto.builder().build());
    }

    @Transactional
    public CompanyProfileDto saveCompanyProfile(CompanyProfileDto dto) {
        requireText(dto.getName(), "Tên công ty");
        CompanyProfile profile = companyProfileRepository.findById(CompanyProfile.SINGLETON_ID)
                .orElseGet(() -> {
                    CompanyProfile fresh = new CompanyProfile();
                    fresh.setId(CompanyProfile.SINGLETON_ID);
                    return fresh;
                });

        profile.setName(dto.getName().trim());
        profile.setShortName(trimToNull(dto.getShortName()));
        profile.setTaxCode(trimToNull(dto.getTaxCode()));
        profile.setAddress(trimToNull(dto.getAddress()));
        profile.setRepresentative(trimToNull(dto.getRepresentative()));
        profile.setRepresentativeTitle(trimToNull(dto.getRepresentativeTitle()));
        profile.setPhone(trimToNull(dto.getPhone()));
        profile.setEmail(trimToNull(dto.getEmail()));
        profile.setWebsite(trimToNull(dto.getWebsite()));
        profile.setBankAccounts(trimToNull(dto.getBankAccounts()));
        profile.setLogoUrl(trimToNull(dto.getLogoUrl()));

        return toCompanyDto(companyProfileRepository.save(profile));
    }

    // ---------------------------------------------------------------- nội bộ

    private void markDefault(DocumentTemplate template) {
        template.setDefaultTemplate(true);
        templateRepository.save(template);
        templateRepository.clearDefaultForKind(template.getKindId(), template.getTemplateId());
    }

    private void applyKind(DocumentKind kind, DocumentKindDto dto) {
        kind.setName(dto.getName().trim());
        kind.setDataSource(parseEnum(DataSourceType.class, dto.getDataSource(), "nguồn dữ liệu"));
        kind.setStage(parseEnum(DocumentStage.class, dto.getStage(), "giai đoạn"));
        kind.setDocNoPattern(trimToNull(dto.getDocNoPattern()));
        kind.setDescription(trimToNull(dto.getDescription()));
        if (dto.getActive() != null) kind.setActive(dto.getActive());
        if (dto.getSortOrder() != null) kind.setSortOrder(dto.getSortOrder());
    }

    /**
     * Đếm số trang và số ô trong bố cục để hiển thị ở danh sách.
     *
     * Bố cục hỏng không được làm hỏng việc lưu: mẫu vẫn lưu được, chỉ là các con
     * số hiển thị về 0. Trình thiết kế mới là chỗ chịu trách nhiệm về tính hợp lệ.
     */
    private void applyLayoutStats(DocumentTemplate template, String layoutJson) {
        template.setPageCount(0);
        template.setFieldCount(0);
        template.setHasBasePdf(false);
        if (layoutJson == null || layoutJson.isBlank()) return;

        try {
            JsonNode root = objectMapper.readTree(layoutJson);
            JsonNode schemas = root.path("schemas");
            if (schemas.isArray()) {
                template.setPageCount(schemas.size());
                int fields = 0;
                for (JsonNode page : schemas) {
                    if (page.isArray()) fields += page.size();
                }
                template.setFieldCount(fields);
            }
            // basePdf là chuỗi khi mẫu dùng nền PDF, là object {width,height,padding}
            // khi mẫu dựng trên giấy trắng.
            template.setHasBasePdf(root.path("basePdf").isTextual());
        } catch (Exception e) {
            log.warn("Bố cục mẫu '{}' không đọc được để đếm trang/ô: {}", template.getName(), e.getMessage());
        }
    }

    private DocumentKindDto toKindDto(DocumentKind kind, int templateCount, String defaultTemplateName) {
        return DocumentKindDto.builder()
                .kindId(kind.getKindId())
                .code(kind.getCode())
                .name(kind.getName())
                .dataSource(kind.getDataSource() == null ? null : kind.getDataSource().name())
                .stage(kind.getStage() == null ? null : kind.getStage().name())
                .docNoPattern(kind.getDocNoPattern())
                .description(kind.getDescription())
                .system(Boolean.TRUE.equals(kind.getSystem()))
                .active(Boolean.TRUE.equals(kind.getActive()))
                .sortOrder(kind.getSortOrder())
                .templateCount(templateCount)
                .defaultTemplateName(defaultTemplateName)
                .build();
    }

    private DocumentTemplateDto toTemplateDto(DocumentTemplate template, DocumentKind kind) {
        return DocumentTemplateDto.builder()
                .templateId(template.getTemplateId())
                .kindId(template.getKindId())
                .kindCode(kind == null ? null : kind.getCode())
                .kindName(kind == null ? null : kind.getName())
                .dataSource(kind == null || kind.getDataSource() == null ? null : kind.getDataSource().name())
                .stage(kind == null || kind.getStage() == null ? null : kind.getStage().name())
                .name(template.getName())
                .layoutJson(template.getLayoutJson())
                .sourceFileName(template.getSourceFileName())
                .paperSize(template.getPaperSize())
                .note(template.getNote())
                .defaultTemplate(Boolean.TRUE.equals(template.getDefaultTemplate()))
                .active(Boolean.TRUE.equals(template.getActive()))
                .version(template.getVersion())
                .build();
    }

    private DocumentTemplateSummaryDto toSummary(DocumentTemplate template) {
        return DocumentTemplateSummaryDto.builder()
                .templateId(template.getTemplateId())
                .kindId(template.getKindId())
                .name(template.getName())
                .sourceFileName(template.getSourceFileName())
                .paperSize(template.getPaperSize())
                .note(template.getNote())
                .defaultTemplate(Boolean.TRUE.equals(template.getDefaultTemplate()))
                .active(Boolean.TRUE.equals(template.getActive()))
                .version(template.getVersion())
                .pageCount(template.getPageCount())
                .fieldCount(template.getFieldCount())
                .hasBasePdf(Boolean.TRUE.equals(template.getHasBasePdf()))
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    private CompanyProfileDto toCompanyDto(CompanyProfile profile) {
        return CompanyProfileDto.builder()
                .name(profile.getName())
                .shortName(profile.getShortName())
                .taxCode(profile.getTaxCode())
                .address(profile.getAddress())
                .representative(profile.getRepresentative())
                .representativeTitle(profile.getRepresentativeTitle())
                .phone(profile.getPhone())
                .email(profile.getEmail())
                .website(profile.getWebsite())
                .bankAccounts(profile.getBankAccounts())
                .logoUrl(profile.getLogoUrl())
                .build();
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String label) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Thiếu " + label);
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Giá trị " + label + " không hợp lệ: " + raw);
        }
    }

    private static String normalizeCode(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " không được để trống");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
