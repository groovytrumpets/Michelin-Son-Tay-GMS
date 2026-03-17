package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.service_ticket_management.api.dto.safety.AdvisorNoteItemRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.SafetyInspectionRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.SafetyInspectionResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.TireDataRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.TireInputRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.InspectionItemRequest;
import com.g42.platform.gms.service_ticket_management.domain.enums.TirePosition;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.InspectionItemResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.WorkCategoryResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.CreateWorkCategoryRequest;
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
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.SafetyInspectionInfraMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.WorkCategoryInfraMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.SafetyInspectionItemRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.SafetyInspectionRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.SafetyInspectionTireRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.WorkCategoryRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.projection.SafetyInspectionItemWithCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
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
    private final ServiceTicketRepository serviceTicketRepository;
    private final SafetyInspectionInfraMapper infraMapper;
    private final WorkCategoryInfraMapper workCategoryInfraMapper;
    private final SafetyInspectionMapper apiMapper;
    private final WorkCategoryApiMapper workCategoryApiMapper;

    /**
     * Get valid work category IDs from database (work_category table)
     * Note: All categories in work_category table (IDs 9001-9013) are safety inspection items
     */
    private List<Integer> getValidWorkCategoryIds() {
        List<SafetyWorkCategoryJpa> categories = workCategoryRepository.findActiveCategories();
        return categories.stream()
                .map(SafetyWorkCategoryJpa::getId)
                .toList();
    }

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
     * Smart update items: UPDATE existing by workCategoryId, DELETE removed, INSERT new
     */
    private void updateItems(Integer inspectionId, List<InspectionItemRequest> requestItems) {
        // Load existing items from database
        List<SafetyInspectionItemJpa> existingItems = itemRepository.findByInspectionId(inspectionId);
        
        if (requestItems == null || requestItems.isEmpty()) {
            // Delete all existing items if request has none
            itemRepository.deleteAll(existingItems);
            return;
        }

        // Convert request to domain
        List<SafetyInspectionItem> newItems = apiMapper.itemsToDomain(requestItems);
        
        // Create map of existing items by workCategoryId for quick lookup
        java.util.Map<Integer, SafetyInspectionItemJpa> existingItemMap = 
            existingItems.stream()
                .collect(java.util.stream.Collectors.toMap(
                    SafetyInspectionItemJpa::getWorkCategoryId,
                    item -> item,
                    (existing, replacement) -> existing
                ));

        // Track which workCategoryIds are in the new request
        java.util.Set<Integer> requestedCategoryIds = new java.util.HashSet<>();
        
        // Update or insert items
        for (SafetyInspectionItem newItem : newItems) {
            newItem.setInspectionId(inspectionId);
            requestedCategoryIds.add(newItem.getWorkCategoryId());
            
            SafetyInspectionItemJpa existingItem = existingItemMap.get(newItem.getWorkCategoryId());
            if (existingItem != null) {
                // UPDATE existing item - keep same itemId
                existingItem.setItemStatus(newItem.getItemStatus());
                // advisorNote chỉ được cập nhật bởi advisor, không phải KTV
                itemRepository.save(existingItem);
            } else {
                // INSERT new item
                SafetyInspectionItemJpa newItemJpa = infraMapper.toJpa(newItem);
                itemRepository.save(newItemJpa);
            }
        }
        
        // DELETE items that are no longer in the request
        for (SafetyInspectionItemJpa existingItem : existingItems) {
            if (!requestedCategoryIds.contains(existingItem.getWorkCategoryId())) {
                itemRepository.delete(existingItem);
            }
        }
    }

    /**
     * Upsert một item lẻ theo workCategoryId.
     * Nếu item đã tồn tại → update itemStatus.
     * Nếu chưa → tạo mới.
     */
    @Transactional
    public InspectionItemResponse upsertItem(Integer inspectionId, InspectionItemRequest request) {
        if (request.getWorkCategoryId() == null) {
            throw new IllegalArgumentException("workCategoryId là bắt buộc");
        }

        SafetyInspectionItemJpa item = itemRepository
                .findByInspectionIdAndWorkCategoryId(inspectionId, request.getWorkCategoryId())
                .orElseGet(() -> {
                    SafetyInspectionItemJpa newItem = new SafetyInspectionItemJpa();
                    newItem.setInspectionId(inspectionId);
                    newItem.setWorkCategoryId(request.getWorkCategoryId());
                    return newItem;
                });

        item.setItemStatus(request.getItemStatus());
        SafetyInspectionItemJpa saved = itemRepository.save(item);

        // Build response with categoryName
        List<SafetyInspectionItemWithCategory> withCategory =
                itemRepository.findByInspectionIdWithCategory(inspectionId);
        return withCategory.stream()
                .filter(p -> p.getItemId().equals(saved.getItemId()))
                .findFirst()
                .map(p -> {
                    InspectionItemResponse resp = new InspectionItemResponse();
                    resp.setItemId(p.getItemId());
                    resp.setWorkCategoryId(p.getWorkCategoryId());
                    resp.setCategoryName(p.getCategoryName());
                    resp.setItemStatus(saved.getItemStatus());
                    resp.setAdvisorNote(saved.getAdvisorNote());
                    return resp;
                })
                .orElseGet(() -> {
                    InspectionItemResponse resp = new InspectionItemResponse();
                    resp.setItemId(saved.getItemId());
                    resp.setWorkCategoryId(saved.getWorkCategoryId());
                    resp.setItemStatus(saved.getItemStatus());
                    resp.setAdvisorNote(saved.getAdvisorNote());
                    return resp;
                });
    }

    /**
     * Get available safety inspection categories from work_category table
     * Note: All categories in work_category table (IDs 9001-9013) are safety inspection items
     */
    @Transactional(readOnly = true)
    public List<WorkCategoryResponse> getSafetyInspectionCategories() {
        List<SafetyWorkCategoryJpa> workCategoryJpas = workCategoryRepository.findActiveCategories();
        List<WorkCategory> workCategories = workCategoryInfraMapper.toDomainList(workCategoryJpas);
        return workCategoryApiMapper.toResponseList(workCategories);
    }

    /**
     * Get only default safety inspection categories (13 fixed items with is_default = 1)
     * These are the standard safety inspection items
     */
    @Transactional(readOnly = true)
    public List<WorkCategoryResponse> getDefaultSafetyInspectionCategories() {
        List<SafetyWorkCategoryJpa> workCategoryJpas = workCategoryRepository.findActiveCategories();
        // Filter only default categories
        List<SafetyWorkCategoryJpa> defaultCategories = workCategoryJpas.stream()
            .filter(cat -> cat.getIsDefault() != null && cat.getIsDefault())
            .toList();
        List<WorkCategory> workCategories = workCategoryInfraMapper.toDomainList(defaultCategories);
        return workCategoryApiMapper.toResponseList(workCategories);
    }

    /**
     * Tạo mới một hạng mục kiểm tra an toàn (work_category) với is_default = false.
     * Dùng khi KTV muốn thêm hạng mục ngoài 13 hạng mục mặc định.
     */
    public WorkCategoryResponse createWorkCategory(CreateWorkCategoryRequest request) {
        // Validate trùng tên
        if (workCategoryRepository.existsByCategoryName(request.getCategoryName())) {
            throw new IllegalArgumentException("Tên hạng mục đã tồn tại: " + request.getCategoryName());
        }

        // Tự generate categoryCode nếu không truyền
        String categoryCode = request.getCategoryCode();
        if (categoryCode == null || categoryCode.isBlank()) {
            categoryCode = request.getCategoryName().toUpperCase()
                    .replace(" ", "_")
                    .replaceAll("[^A-Z0-9_]", "");
        }

        // Validate trùng code
        if (workCategoryRepository.existsByCategoryCode(categoryCode)) {
            throw new IllegalArgumentException("Mã hạng mục đã tồn tại: " + categoryCode);
        }

        SafetyWorkCategoryJpa jpa = new SafetyWorkCategoryJpa();
        jpa.setCategoryName(request.getCategoryName());
        jpa.setCategoryCode(categoryCode);
        jpa.setDisplayOrder(request.getDisplayOrder());
        jpa.setIsActive(true);
        jpa.setIsDefault(false);

        SafetyWorkCategoryJpa saved = workCategoryRepository.save(jpa);
        WorkCategory domain = workCategoryInfraMapper.toDomain(saved);
        return workCategoryApiMapper.toResponse(domain);
    }

    /**
     * Advisor cập nhật ghi chú cho nhiều hạng mục kiểm tra cùng lúc (upsert theo workCategoryId).
     * - Enable: item đã có sẵn → update advisorNote
     * - Skip: item chưa có → tạo mới item với advisorNote
     */
    public List<InspectionItemResponse> updateAdvisorNotes(Integer inspectionId, List<AdvisorNoteItemRequest> noteItems) {
        return noteItems.stream().map(noteItem -> {
            if (noteItem.getWorkCategoryId() == null) {
                throw new IllegalArgumentException("workCategoryId là bắt buộc");
            }

            SafetyInspectionItemJpa item = itemRepository
                    .findByInspectionIdAndWorkCategoryId(inspectionId, noteItem.getWorkCategoryId())
                    .orElseGet(() -> {
                        SafetyInspectionItemJpa newItem = new SafetyInspectionItemJpa();
                        newItem.setInspectionId(inspectionId);
                        newItem.setWorkCategoryId(noteItem.getWorkCategoryId());
                        return newItem;
                    });

            item.setAdvisorNote(noteItem.getAdvisorNote());
            SafetyInspectionItemJpa saved = itemRepository.save(item);

            List<SafetyInspectionItemWithCategory> withCategory =
                    itemRepository.findByInspectionIdWithCategory(inspectionId);

            return withCategory.stream()
                    .filter(p -> p.getItemId().equals(saved.getItemId()))
                    .findFirst()
                    .map(p -> {
                        InspectionItemResponse resp = new InspectionItemResponse();
                        resp.setItemId(p.getItemId());
                        resp.setWorkCategoryId(p.getWorkCategoryId());
                        resp.setCategoryName(p.getCategoryName());
                        resp.setItemStatus(saved.getItemStatus());
                        resp.setAdvisorNote(saved.getAdvisorNote());
                        return resp;
                    })
                    .orElseGet(() -> {
                        InspectionItemResponse resp = new InspectionItemResponse();
                        resp.setItemId(saved.getItemId());
                        resp.setWorkCategoryId(saved.getWorkCategoryId());
                        resp.setItemStatus(saved.getItemStatus());
                        resp.setAdvisorNote(saved.getAdvisorNote());
                        return resp;
                    });
        }).toList();
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

        // Validate inspection items against database using work_category_id
        if (request.getItems() != null) {
            List<Integer> validCategoryIds = getValidWorkCategoryIds();
            if (validCategoryIds.isEmpty()) {
                throw new IllegalStateException("No active safety inspection categories found in database");
            }

            if (request.getItems().size() > validCategoryIds.size()) {
                throw new IllegalArgumentException("Maximum " + validCategoryIds.size() + " inspection items allowed");
            }

            for (InspectionItemRequest item : request.getItems()) {
                if (item.getWorkCategoryId() == null) {
                    throw new IllegalArgumentException("Work category ID is required for inspection item");
                }
                if (!validCategoryIds.contains(item.getWorkCategoryId())) {
                    throw new IllegalArgumentException("Invalid work category ID: " + item.getWorkCategoryId());
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
