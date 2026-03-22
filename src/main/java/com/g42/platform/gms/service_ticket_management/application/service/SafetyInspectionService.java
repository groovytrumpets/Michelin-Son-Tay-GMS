package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.service_ticket_management.api.dto.safety.AdvisorNoteItemRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.AddCustomCategoryRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.SafetyInspectionRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.SafetyInspectionResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.TireDataRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.TireInputRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.InspectionItemRequest;
import com.g42.platform.gms.service_ticket_management.domain.enums.TirePosition;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.InspectionItemResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.WorkCategoryResponse;
import com.g42.platform.gms.service_ticket_management.api.mapper.SafetyInspectionMapper;
import com.g42.platform.gms.service_ticket_management.api.mapper.WorkCategoryApiMapper;
import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspection;
import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspectionItem;
import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspectionTire;
import com.g42.platform.gms.service_ticket_management.domain.entity.WorkCategory;
import com.g42.platform.gms.service_ticket_management.domain.enums.InspectionStatus;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyInspectionJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyInspectionItemJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyInspectionTireJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyWorkCategoryJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.TicketCustomCategoryJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.SafetyInspectionInfraMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.WorkCategoryInfraMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.SafetyInspectionItemRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.SafetyInspectionRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.SafetyInspectionTireRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.TicketCustomCategoryRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.WorkCategoryRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.projection.SafetyInspectionItemWithCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SafetyInspectionService {

    private final SafetyInspectionRepository inspectionRepository;
    private final SafetyInspectionTireRepository tireRepository;
    private final SafetyInspectionItemRepository itemRepository;
    private final WorkCategoryRepository workCategoryRepository;
    private final TicketCustomCategoryRepository customCategoryRepository;
    private final ServiceTicketRepository serviceTicketRepository;
    private final SafetyInspectionInfraMapper infraMapper;
    private final WorkCategoryInfraMapper workCategoryInfraMapper;
    private final SafetyInspectionMapper apiMapper;
    private final WorkCategoryApiMapper workCategoryApiMapper;

    /**
     * Enable inspection by ticket code (create PENDING record)
     */
    public SafetyInspectionResponse enableInspectionByCode(String ticketCode, Integer technicianId) {
        ServiceTicketJpa serviceTicket = serviceTicketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new IllegalArgumentException("Service ticket not found with code: " + ticketCode));

        return enableInspection(serviceTicket.getServiceTicketId(), technicianId);
    }

    /**
     * Skip inspection by ticket code (create SKIPPED record)
     */
    public SafetyInspectionResponse skipInspectionByCode(String ticketCode) {
        ServiceTicketJpa serviceTicket = serviceTicketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new IllegalArgumentException("Service ticket not found with code: " + ticketCode));

        return skipInspection(serviceTicket.getServiceTicketId());
    }

    /**
     * Get inspection by ticket code
     */
    @Transactional(readOnly = true)
    public SafetyInspectionResponse getInspectionByTicketCode(String ticketCode) {
        ServiceTicketJpa serviceTicket = serviceTicketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new IllegalArgumentException("Service ticket not found with code: " + ticketCode));

        return getInspectionByServiceTicket(serviceTicket.getServiceTicketId());
    }

    /**
     * Enable inspection (create PENDING record + tạo 13 items mặc định)
     */
    public SafetyInspectionResponse enableInspection(Integer serviceTicketId, Integer technicianId) {
        // Check if inspection already exists
        Optional<SafetyInspectionJpa> existing = inspectionRepository.findByServiceTicketId(serviceTicketId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Inspection already exists for service ticket: " + serviceTicketId);
        }

        // Validate technicianId
        if (technicianId == null) {
            throw new IllegalArgumentException("Technician ID is required to enable inspection");
        }

        // Create new PENDING inspection
        SafetyInspection domain = new SafetyInspection();
        domain.setServiceTicketId(serviceTicketId);
        domain.setTechnicianId(technicianId);
        domain.setInspectionStatus(InspectionStatus.PENDING);
        domain.initializeDefaults();

        SafetyInspectionJpa jpa = infraMapper.toJpa(domain);
        SafetyInspectionJpa saved = inspectionRepository.save(jpa);

        // Tạo 13 items mặc định (itemStatus = null, advisorNote = null)
        List<SafetyWorkCategoryJpa> defaultCategories = workCategoryRepository.findActiveCategories().stream()
                .filter(cat -> cat.getIsDefault() != null && cat.getIsDefault())
                .toList();
        for (SafetyWorkCategoryJpa cat : defaultCategories) {
            SafetyInspectionItemJpa item = new SafetyInspectionItemJpa();
            item.setInspectionId(saved.getInspectionId());
            item.setWorkCategoryId(cat.getId());
            itemRepository.save(item);
        }

        // Update safety_inspection_enabled flag on service ticket
        ServiceTicketJpa ticket = serviceTicketRepository.findById(serviceTicketId)
                .orElseThrow(() -> new IllegalArgumentException("Service ticket not found: " + serviceTicketId));
        ticket.setSafetyInspectionEnabled(true);
        serviceTicketRepository.save(ticket);

        return getInspectionByServiceTicket(serviceTicketId);
    }


    /**
     * Skip inspection (create SKIPPED record, không có items)
     */
    public SafetyInspectionResponse skipInspection(Integer serviceTicketId) {
        Optional<SafetyInspectionJpa> existing = inspectionRepository.findByServiceTicketId(serviceTicketId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Inspection already exists for service ticket: " + serviceTicketId);
        }

        SafetyInspection domain = new SafetyInspection();
        domain.setServiceTicketId(serviceTicketId);
        domain.setTechnicianId(null);
        domain.setInspectionStatus(InspectionStatus.SKIPPED);
        domain.initializeDefaults();

        SafetyInspectionJpa jpa = infraMapper.toJpa(domain);
        SafetyInspectionJpa saved = inspectionRepository.save(jpa);

        SafetyInspection savedDomain = infraMapper.toDomain(saved);
        return apiMapper.toResponse(savedDomain);
    }

    /**
     * Save inspection data (create or update COMPLETED record)
     */
    public SafetyInspectionResponse saveInspectionData(SafetyInspectionRequest request, Integer technicianId) {
        validateInspectionData(request);

        SafetyInspection domain = apiMapper.toDomain(request);
        domain.setTechnicianId(technicianId);
        domain.setInspectionStatus(InspectionStatus.COMPLETED);

        // Check if inspection exists
        Optional<SafetyInspectionJpa> existing = inspectionRepository.findByServiceTicketId(request.getServiceTicketId());

        SafetyInspectionJpa saved;
        if (existing.isPresent()) {
            // Update existing
            SafetyInspectionJpa existingJpa = existing.get();
            existingJpa.setGeneralNotes(request.getGeneralNotes());
            existingJpa.setTechnicianNotes(request.getTechnicianNotes());
            existingJpa.setTechnicianId(technicianId);
            existingJpa.setInspectionStatus(InspectionStatus.COMPLETED);
            existingJpa.setUpdatedAt(LocalDateTime.now());

            saved = inspectionRepository.save(existingJpa);
        } else {
            // Create new
            domain.initializeDefaults();
            SafetyInspectionJpa jpa = infraMapper.toJpa(domain);
            saved = inspectionRepository.save(jpa);
        }

        // Smart update tires (giữ nguyên tireId nếu đã có)
        updateTires(saved.getInspectionId(), expandTires(request.getTires()));

        // Smart update items (giữ nguyên itemId nếu đã có, bảo toàn advisorNote)
        updateItems(saved.getInspectionId(), request.getItems());

        return getInspectionByServiceTicket(request.getServiceTicketId());
    }

    /**
     * Get inspection by inspection ID
     */
    @Transactional(readOnly = true)
    public SafetyInspectionResponse getInspectionById(Integer inspectionId) {
        SafetyInspectionJpa inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found with ID: " + inspectionId));

        SafetyInspection domain = infraMapper.toDomain(inspection);

        // Load tires
        List<SafetyInspectionTireJpa> tires = tireRepository.findByInspectionId(inspectionId);
        domain.setTires(infraMapper.tiresToDomain(tires));

        // Load items with category names using JOIN query
        domain.setItems(infraMapper.projectionsToItems(
            itemRepository.findByInspectionIdWithCategory(inspectionId)
        ));

        return apiMapper.toResponse(domain);
    }

    /**
     * Get inspection by service ticket ID
     */
    @Transactional(readOnly = true)
    public SafetyInspectionResponse getInspectionByServiceTicket(Integer serviceTicketId) {
        SafetyInspectionJpa inspection = inspectionRepository.findByServiceTicketId(serviceTicketId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found for service ticket: " + serviceTicketId));

        SafetyInspection domain = infraMapper.toDomain(inspection);

        // Load tires
        List<SafetyInspectionTireJpa> tires = tireRepository.findByInspectionId(inspection.getInspectionId());
        domain.setTires(infraMapper.tiresToDomain(tires));

        // Load items with category names using JOIN query
        domain.setItems(infraMapper.projectionsToItems(
            itemRepository.findByInspectionIdWithCategory(inspection.getInspectionId())
        ));

        return apiMapper.toResponse(domain);
    }

    /**
     * Update existing inspection
     */
    public SafetyInspectionResponse updateInspectionData(Integer inspectionId, SafetyInspectionRequest request) {
        validateInspectionData(request);

        SafetyInspectionJpa inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));

        // Update main inspection
        inspection.setGeneralNotes(request.getGeneralNotes());
        inspection.setTechnicianNotes(request.getTechnicianNotes());
        inspection.setInspectionStatus(InspectionStatus.COMPLETED);
        inspection.setUpdatedAt(LocalDateTime.now());

        SafetyInspectionJpa saved = inspectionRepository.save(inspection);

        // Smart update tires: UPDATE existing, DELETE removed, INSERT new
        updateTires(inspectionId, expandTires(request.getTires()));

        // Smart update items: UPDATE existing, DELETE removed, INSERT new
        updateItems(inspectionId, request.getItems());

        return getInspectionByServiceTicket(saved.getServiceTicketId());
    }

    /**
     * Smart update tires: UPDATE existing by tirePosition, DELETE removed, INSERT new
     */
    private void updateTires(Integer inspectionId, List<TireDataRequest> requestTires) {
        // Load existing tires from database
        List<SafetyInspectionTireJpa> existingTires = tireRepository.findByInspectionId(inspectionId);
        
        if (requestTires == null || requestTires.isEmpty()) {
            // Delete all existing tires if request has none
            tireRepository.deleteAll(existingTires);
            return;
        }

        // Convert request to domain
        List<SafetyInspectionTire> newTires = apiMapper.tiresToDomain(requestTires);
        
        // Create map of existing tires by tirePosition for quick lookup
        java.util.Map<com.g42.platform.gms.service_ticket_management.domain.enums.TirePosition, SafetyInspectionTireJpa> existingTireMap = 
            existingTires.stream()
                .collect(java.util.stream.Collectors.toMap(
                    SafetyInspectionTireJpa::getTirePosition,
                    tire -> tire,
                    (existing, replacement) -> existing
                ));

        // Track which tirePositions are in the new request
        java.util.Set<com.g42.platform.gms.service_ticket_management.domain.enums.TirePosition> requestedPositions = new java.util.HashSet<>();
        
        // Update or insert tires
        for (SafetyInspectionTire newTire : newTires) {
            newTire.setInspectionId(inspectionId);
            requestedPositions.add(newTire.getTirePosition());
            
            SafetyInspectionTireJpa existingTire = existingTireMap.get(newTire.getTirePosition());
            if (existingTire != null) {
                // UPDATE existing tire - keep same tireId
                existingTire.setTreadDepth(newTire.getTreadDepth());
                existingTire.setPressure(newTire.getPressure());
                existingTire.setPressureUnit(newTire.getPressureUnit());
                existingTire.setTireSpecification(newTire.getTireSpecification());
                existingTire.setRecommendedTireSize(newTire.getRecommendedTireSize());
                existingTire.setRecommendedPressure(newTire.getRecommendedPressure());
                existingTire.setRecommendedPressureUnit(newTire.getRecommendedPressureUnit());
                tireRepository.save(existingTire);
            } else {
                // INSERT new tire
                SafetyInspectionTireJpa newTireJpa = infraMapper.toJpa(newTire);
                tireRepository.save(newTireJpa);
            }
        }
        
        // DELETE tires that are no longer in the request
        for (SafetyInspectionTireJpa existingTire : existingTires) {
            if (!requestedPositions.contains(existingTire.getTirePosition())) {
                tireRepository.delete(existingTire);
            }
        }
    }

    /**
     * Smart update items: UPDATE existing by workCategoryId or customCategoryId, DELETE removed, INSERT new
     */
    private void updateItems(Integer inspectionId, List<InspectionItemRequest> requestItems) {
        List<SafetyInspectionItemJpa> existingItems = itemRepository.findByInspectionId(inspectionId);
        
        if (requestItems == null || requestItems.isEmpty()) {
            itemRepository.deleteAll(existingItems);
            return;
        }

        List<SafetyInspectionItem> newItems = apiMapper.itemsToDomain(requestItems);
        
        // Map existing by workCategoryId (default) và customCategoryId (custom)
        java.util.Map<Integer, SafetyInspectionItemJpa> byWorkCategory = existingItems.stream()
                .filter(i -> i.getWorkCategoryId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        SafetyInspectionItemJpa::getWorkCategoryId, i -> i, (a, b) -> a));
        java.util.Map<Integer, SafetyInspectionItemJpa> byCustomCategory = existingItems.stream()
                .filter(i -> i.getCustomCategoryId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        SafetyInspectionItemJpa::getCustomCategoryId, i -> i, (a, b) -> a));

        java.util.Set<Integer> touchedItemIds = new java.util.HashSet<>();

        for (SafetyInspectionItem newItem : newItems) {
            newItem.setInspectionId(inspectionId);
            SafetyInspectionItemJpa existing = null;

            if (newItem.getWorkCategoryId() != null) {
                existing = byWorkCategory.get(newItem.getWorkCategoryId());
            } else if (newItem.getCustomCategoryId() != null) {
                existing = byCustomCategory.get(newItem.getCustomCategoryId());
            }

            if (existing != null) {
                existing.setItemStatus(newItem.getItemStatus());
                itemRepository.save(existing);
                touchedItemIds.add(existing.getItemId());
            } else {
                SafetyInspectionItemJpa newItemJpa = infraMapper.toJpa(newItem);
                SafetyInspectionItemJpa saved = itemRepository.save(newItemJpa);
                touchedItemIds.add(saved.getItemId());
            }
        }

        // Xóa các item không còn trong request (chỉ xóa custom, giữ default)
        for (SafetyInspectionItemJpa existingItem : existingItems) {
            if (!touchedItemIds.contains(existingItem.getItemId()) && existingItem.getCustomCategoryId() != null) {
                itemRepository.delete(existingItem);
            }
        }
    }

    /**
     * Lấy danh sách tất cả hạng mục kiểm tra của một phiếu (13 default + hạng mục phụ).
     */
    @Transactional(readOnly = true)
    public List<InspectionItemResponse> getInspectionItems(Integer inspectionId) {
        return itemRepository.findByInspectionIdWithCategory(inspectionId).stream()
                .map(p -> {
                    InspectionItemResponse resp = new InspectionItemResponse();
                    resp.setItemId(p.getItemId());
                    resp.setWorkCategoryId(p.getWorkCategoryId());
                    resp.setCustomCategoryId(p.getCustomCategoryId());
                    resp.setCategoryName(p.getCategoryName());
                    resp.setItemStatus(p.getItemStatus());
                    resp.setAdvisorNote(p.getAdvisorNote());
                    return resp;
                })
                .toList();
    }

    /**
     * Bulk upsert itemStatus cho nhiều hạng mục cùng lúc (tech điền).
     */
    @Transactional
    public List<InspectionItemResponse> upsertItems(Integer inspectionId, List<InspectionItemRequest> requests) {
        return requests.stream()
                .map(req -> upsertItemInternal(inspectionId, req))
                .toList();
    }

    private InspectionItemResponse upsertItemInternal(Integer inspectionId, InspectionItemRequest request) {
        if (request.getWorkCategoryId() == null && request.getCustomCategoryId() == null) {
            throw new IllegalArgumentException("workCategoryId hoặc customCategoryId là bắt buộc");
        }

        SafetyInspectionItemJpa item;
        if (request.getWorkCategoryId() != null) {
            item = itemRepository
                    .findByInspectionIdAndWorkCategoryId(inspectionId, request.getWorkCategoryId())
                    .orElseGet(() -> {
                        SafetyInspectionItemJpa newItem = new SafetyInspectionItemJpa();
                        newItem.setInspectionId(inspectionId);
                        newItem.setWorkCategoryId(request.getWorkCategoryId());
                        return newItem;
                    });
        } else {
            item = itemRepository
                    .findByInspectionIdAndCustomCategoryId(inspectionId, request.getCustomCategoryId())
                    .orElseGet(() -> {
                        SafetyInspectionItemJpa newItem = new SafetyInspectionItemJpa();
                        newItem.setInspectionId(inspectionId);
                        newItem.setCustomCategoryId(request.getCustomCategoryId());
                        return newItem;
                    });
        }

        item.setItemStatus(request.getItemStatus());
        SafetyInspectionItemJpa saved = itemRepository.save(item);

        return buildItemResponse(saved, inspectionId);
    }

    /**
     * Get only default safety inspection categories (13 fixed items with is_default = 1)
     */
    @Transactional(readOnly = true)
    public List<WorkCategoryResponse> getDefaultSafetyInspectionCategories() {
        List<SafetyWorkCategoryJpa> defaultCategories = workCategoryRepository.findActiveCategories().stream()
            .filter(cat -> cat.getIsDefault() != null && cat.getIsDefault())
            .toList();
        List<WorkCategory> workCategories = workCategoryInfraMapper.toDomainList(defaultCategories);
        return workCategoryApiMapper.toResponseList(workCategories);
    }

    /**
     * Advisor cập nhật ghi chú cho nhiều hạng mục kiểm tra cùng lúc.
     * Hỗ trợ cả default (workCategoryId) và custom (customCategoryId).
     */
    public List<InspectionItemResponse> updateAdvisorNotes(Integer inspectionId, List<AdvisorNoteItemRequest> noteItems) {
        return noteItems.stream().map(noteItem -> {
            if (noteItem.getWorkCategoryId() == null && noteItem.getCustomCategoryId() == null) {
                throw new IllegalArgumentException("workCategoryId hoặc customCategoryId là bắt buộc");
            }

            SafetyInspectionItemJpa item;
            if (noteItem.getWorkCategoryId() != null) {
                item = itemRepository
                        .findByInspectionIdAndWorkCategoryId(inspectionId, noteItem.getWorkCategoryId())
                        .orElseGet(() -> {
                            SafetyInspectionItemJpa newItem = new SafetyInspectionItemJpa();
                            newItem.setInspectionId(inspectionId);
                            newItem.setWorkCategoryId(noteItem.getWorkCategoryId());
                            return newItem;
                        });
            } else {
                item = itemRepository
                        .findByInspectionIdAndCustomCategoryId(inspectionId, noteItem.getCustomCategoryId())
                        .orElseGet(() -> {
                            SafetyInspectionItemJpa newItem = new SafetyInspectionItemJpa();
                            newItem.setInspectionId(inspectionId);
                            newItem.setCustomCategoryId(noteItem.getCustomCategoryId());
                            return newItem;
                        });
            }

            item.setAdvisorNote(noteItem.getAdvisorNote());
            SafetyInspectionItemJpa saved = itemRepository.save(item);
            return buildItemResponse(saved, inspectionId);
        }).toList();
    }

    /**
     * Thêm hạng mục tùy chỉnh vào phiếu kiểm tra an toàn.
     * Insert vào ticket_custom_category (không ảnh hưởng work_category).
     * Tự động tạo item tương ứng trong safety_inspection_item.
     */
    @Transactional
    public InspectionItemResponse addCustomCategory(Integer inspectionId, AddCustomCategoryRequest request) {
        inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu kiểm tra: " + inspectionId));

        if (customCategoryRepository.existsByInspectionIdAndCategoryName(inspectionId, request.getCategoryName())) {
            throw new IllegalArgumentException("Hạng mục '" + request.getCategoryName() + "' đã tồn tại trong phiếu này");
        }

        TicketCustomCategoryJpa customCat = new TicketCustomCategoryJpa();
        customCat.setInspectionId(inspectionId);
        customCat.setCategoryName(request.getCategoryName());
        customCat.setDisplayOrder(request.getDisplayOrder());
        TicketCustomCategoryJpa savedCat = customCategoryRepository.save(customCat);

        SafetyInspectionItemJpa item = new SafetyInspectionItemJpa();
        item.setInspectionId(inspectionId);
        item.setCustomCategoryId(savedCat.getId());
        SafetyInspectionItemJpa savedItem = itemRepository.save(item);

        InspectionItemResponse resp = new InspectionItemResponse();
        resp.setItemId(savedItem.getItemId());
        resp.setCustomCategoryId(savedCat.getId());
        resp.setCategoryName(savedCat.getCategoryName());
        return resp;
    }

    /** Helper: build InspectionItemResponse từ saved item + JOIN query để lấy categoryName */
    private InspectionItemResponse buildItemResponse(SafetyInspectionItemJpa saved, Integer inspectionId) {
        List<SafetyInspectionItemWithCategory> withCategory =
                itemRepository.findByInspectionIdWithCategory(inspectionId);
        return withCategory.stream()
                .filter(p -> p.getItemId().equals(saved.getItemId()))
                .findFirst()
                .map(p -> {
                    InspectionItemResponse resp = new InspectionItemResponse();
                    resp.setItemId(p.getItemId());
                    resp.setWorkCategoryId(p.getWorkCategoryId());
                    resp.setCustomCategoryId(p.getCustomCategoryId());
                    resp.setCategoryName(p.getCategoryName());
                    resp.setItemStatus(saved.getItemStatus());
                    resp.setAdvisorNote(saved.getAdvisorNote());
                    return resp;
                })
                .orElseGet(() -> {
                    InspectionItemResponse resp = new InspectionItemResponse();
                    resp.setItemId(saved.getItemId());
                    resp.setWorkCategoryId(saved.getWorkCategoryId());
                    resp.setCustomCategoryId(saved.getCustomCategoryId());
                    resp.setItemStatus(saved.getItemStatus());
                    resp.setAdvisorNote(saved.getAdvisorNote());
                    return resp;
                });
    }

    /**
     * Validate inspection data
     */
    private void validateInspectionData(SafetyInspectionRequest request) {
        if (request.getServiceTicketId() == null) {
            throw new IllegalArgumentException("Service ticket ID is required");
        }

        // Validate tire data (new structured format)
        if (request.getTires() != null) {
            TireInputRequest t = request.getTires();
            validateActualTire("frontLeft",  t.getFrontLeft());
            validateActualTire("frontRight", t.getFrontRight());
            validateActualTire("rearLeft",   t.getRearLeft());
            validateActualTire("rearRight",  t.getRearRight());
            validateActualTire("spare",      t.getSpare());
        }

        // Validate inspection items
        if (request.getItems() != null) {
            for (InspectionItemRequest item : request.getItems()) {
                if (item.getWorkCategoryId() == null && item.getCustomCategoryId() == null) {
                    throw new IllegalArgumentException("workCategoryId hoặc customCategoryId là bắt buộc cho mỗi hạng mục");
                }
            }
        }
    }

    private void validateActualTire(String label, TireInputRequest.TireActualData tire) {
        if (tire == null) return;
        if (tire.getTreadDepth() != null && tire.getTreadDepth().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + " tread depth must be non-negative");
        }
        if (tire.getPressure() != null && tire.getPressure().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + " pressure must be non-negative");
        }
    }

    /**
     * Expand structured TireInputRequest into 5 flat TireDataRequest records.
     *
     * tireSpecification (size thực tế, theo trục):
     *   FRONT_LEFT + FRONT_RIGHT → frontTireSpecification
     *   REAR_LEFT  + REAR_RIGHT  → rearTireSpecification
     *   SPARE                    → null
     *
     * recommendedTireSize: nhập 1 lần, áp cho FRONT_LEFT, FRONT_RIGHT, REAR_LEFT, REAR_RIGHT (SPARE = null)
     *
     * recommendedPressure (theo trục):
     *   FRONT_LEFT + FRONT_RIGHT → frontRecommendedPressure
     *   REAR_LEFT  + REAR_RIGHT + SPARE → rearRecommendedPressure
     */
    private List<TireDataRequest> expandTires(TireInputRequest input) {
        if (input == null) return List.of();

        String frontSpec   = input.getFrontTireSpecification();
        String rearSpec    = input.getRearTireSpecification();
        String recSize     = input.getRecommendedTireSize();
        BigDecimal frontRec = input.getFrontRecommendedPressure();
        BigDecimal rearRec  = input.getRearRecommendedPressure();
        BigDecimal spareRec = input.getSpareRecommendedPressure();

        List<TireDataRequest> result = new java.util.ArrayList<>();
        result.add(buildTire(TirePosition.FRONT_LEFT,  input.getFrontLeft(),  frontSpec, recSize,  frontRec));
        result.add(buildTire(TirePosition.FRONT_RIGHT, input.getFrontRight(), frontSpec, recSize,  frontRec));
        result.add(buildTire(TirePosition.REAR_LEFT,   input.getRearLeft(),   rearSpec,  recSize,  rearRec));
        result.add(buildTire(TirePosition.REAR_RIGHT,  input.getRearRight(),  rearSpec,  recSize,  rearRec));
        result.add(buildTire(TirePosition.SPARE,       input.getSpare(),      null,      null,     spareRec));
        return result;
    }

    private TireDataRequest buildTire(TirePosition position,
                                      TireInputRequest.TireActualData actual,
                                      String tireSpecification,
                                      String recommendedTireSize,
                                      BigDecimal recommendedPressure) {
        TireDataRequest req = new TireDataRequest();
        req.setTirePosition(position);
        if (actual != null) {
            req.setTreadDepth(actual.getTreadDepth());
            req.setPressure(actual.getPressure());
            req.setPressureUnit(actual.getPressureUnit());
        }
        req.setTireSpecification(tireSpecification);
        req.setRecommendedTireSize(recommendedTireSize);
        req.setRecommendedPressure(recommendedPressure);
        return req;
    }
}
