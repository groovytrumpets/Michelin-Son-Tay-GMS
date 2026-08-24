package com.g42.platform.gms.catalogdraft.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.catalogdraft.api.dto.CatalogDraftRequest;
import com.g42.platform.gms.catalogdraft.api.dto.CatalogDraftResponse;
import com.g42.platform.gms.catalogdraft.infrastructure.entity.CatalogDraftJpa;
import com.g42.platform.gms.catalogdraft.infrastructure.repository.CatalogDraftRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CatalogDraftService {
    private static final Set<String> ALLOWED_TYPES = Set.of("PRODUCT", "SERVICE", "COMBO");
    private static final int MAX_PAYLOAD_LENGTH = 2_000_000;

    private final CatalogDraftRepository repository;
    private final ObjectMapper objectMapper;

    public CatalogDraftService(CatalogDraftRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<CatalogDraftResponse> list(Integer staffId, String draftType) {
        String normalizedType = normalizeType(draftType);
        return repository.findByStaffIdAndDraftTypeOrderByUpdatedAtDesc(staffId, normalizedType)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public CatalogDraftResponse create(Integer staffId, CatalogDraftRequest request) {
        CatalogDraftJpa entity = new CatalogDraftJpa();
        apply(entity, staffId, request, null);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public CatalogDraftResponse update(Integer staffId, Long draftId, CatalogDraftRequest request) {
        CatalogDraftJpa entity = ownedDraft(staffId, draftId);
        apply(entity, staffId, request, entity.getDraftType());
        return toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Integer staffId, Long draftId) {
        repository.delete(ownedDraft(staffId, draftId));
    }

    private CatalogDraftJpa ownedDraft(Integer staffId, Long draftId) {
        requireStaff(staffId);
        return repository.findByDraftIdAndStaffId(draftId, staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bản nháp."));
    }

    private void apply(CatalogDraftJpa entity, Integer staffId, CatalogDraftRequest request, String existingType) {
        requireStaff(staffId);
        if (request == null || request.getPayload() == null || request.getPayload().isNull()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu nội dung bản nháp.");
        }
        String type = normalizeType(request.getDraftType());
        if (existingType != null && !existingType.equals(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể đổi loại của bản nháp.");
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(request.getPayload());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung bản nháp không hợp lệ.", e);
        }
        if (json.length() > MAX_PAYLOAD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Bản nháp vượt quá dung lượng cho phép.");
        }
        String title = request.getTitle() == null ? "" : request.getTitle().trim();
        if (title.isEmpty()) title = "Bản nháp chưa đặt tên";
        if (title.length() > 255) title = title.substring(0, 255);

        entity.setStaffId(staffId);
        entity.setDraftType(type);
        entity.setTitle(title);
        entity.setPayloadJson(json);
    }

    private CatalogDraftResponse toResponse(CatalogDraftJpa entity) {
        JsonNode payload;
        try {
            payload = objectMapper.readTree(entity.getPayloadJson());
        } catch (Exception e) {
            payload = objectMapper.createObjectNode();
        }
        return CatalogDraftResponse.builder()
                .draftId(entity.getDraftId())
                .draftType(entity.getDraftType())
                .title(entity.getTitle())
                .payload(payload)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String normalizeType(String draftType) {
        String normalized = draftType == null ? "" : draftType.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại bản nháp không hợp lệ.");
        }
        return normalized;
    }

    private void requireStaff(Integer staffId) {
        if (staffId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác định được nhân viên đăng nhập.");
        }
    }
}
