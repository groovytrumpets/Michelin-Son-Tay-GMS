package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.service_ticket_management.api.dto.safety.SafetyInspectionRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.SafetyInspectionResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.TireDataRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.InspectionItemRequest;
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
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.SafetyInspectionInfraMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.WorkCategoryInfraMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.SafetyInspectionItemRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.SafetyInspectionRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.SafetyInspectionTireRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.WorkCategoryRepository;
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
    public SafetyInspectionResponse skipInspectionByCode(String ticketCode, String reason) {
        ServiceTicketJpa serviceTicket = serviceTicketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new IllegalArgumentException("Service ticket not found with code: " + ticketCode));

        return skipInspection(serviceTicket.getServiceTicketId(), reason);
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
     * Enable inspection (create PENDING record)
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

        // Create new PENDING inspection (no default items - technician will add them when saving)
        SafetyInspection domain = new SafetyInspection();
        domain.setServiceTicketId(serviceTicketId);
        domain.setTechnicianId(technicianId);
        domain.setInspectionStatus(InspectionStatus.PENDING);
        domain.initializeDefaults();

        SafetyInspectionJpa jpa = infraMapper.toJpa(domain);
        SafetyInspectionJpa saved = inspectionRepository.save(jpa);

        SafetyInspection savedDomain = infraMapper.toDomain(saved);
        return apiMapper.toResponse(savedDomain);
    }


    /**
     * Skip inspection (create SKIPPED record)
     */
    public SafetyInspectionResponse skipInspection(Integer serviceTicketId, String reason) {
        // Check if inspection already exists
        Optional<SafetyInspectionJpa> existing = inspectionRepository.findByServiceTicketId(serviceTicketId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Inspection already exists for service ticket: " + serviceTicketId);
        }

        // Create new SKIPPED inspection
        SafetyInspection domain = new SafetyInspection();
        domain.setServiceTicketId(serviceTicketId);
        domain.setTechnicianId(null);
        domain.setInspectionStatus(InspectionStatus.SKIPPED);
        domain.setGeneralNotes(reason);
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
            existingJpa.setInspectionStatus(InspectionStatus.COMPLETED);
            existingJpa.setUpdatedAt(LocalDateTime.now());

            saved = inspectionRepository.save(existingJpa);

            // Delete existing tires and items
            tireRepository.deleteByInspectionId(saved.getInspectionId());
            itemRepository.deleteByInspectionId(saved.getInspectionId());
        } else {
            // Create new
            domain.initializeDefaults();
            SafetyInspectionJpa jpa = infraMapper.toJpa(domain);
            saved = inspectionRepository.save(jpa);
        }

        // Save tires
        if (request.getTires() != null && !request.getTires().isEmpty()) {
            List<SafetyInspectionTire> tires = apiMapper.tiresToDomain(request.getTires());
            for (SafetyInspectionTire tire : tires) {
                tire.setInspectionId(saved.getInspectionId());
            }
            List<SafetyInspectionTireJpa> tireJpas = infraMapper.tiresToJpa(tires);
            tireRepository.saveAll(tireJpas);
        }

        // Save items
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<SafetyInspectionItem> items = apiMapper.itemsToDomain(request.getItems());
            for (SafetyInspectionItem item : items) {
                item.setInspectionId(saved.getInspectionId());
            }
            List<SafetyInspectionItemJpa> itemJpas = infraMapper.itemsToJpa(items);
            itemRepository.saveAll(itemJpas);
        }

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
        inspection.setInspectionStatus(InspectionStatus.COMPLETED);
        inspection.setUpdatedAt(LocalDateTime.now());

        SafetyInspectionJpa saved = inspectionRepository.save(inspection);

        // Smart update tires: UPDATE existing, DELETE removed, INSERT new
        updateTires(inspectionId, request.getTires());

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
                existingItem.setNotes(newItem.getNotes());
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
     * Validate inspection data
     */
    private void validateInspectionData(SafetyInspectionRequest request) {
        if (request.getServiceTicketId() == null) {
            throw new IllegalArgumentException("Service ticket ID is required");
        }

        // Validate tire data
        if (request.getTires() != null) {
            if (request.getTires().size() > 4) {
                throw new IllegalArgumentException("Maximum 4 tire records allowed");
            }

            for (TireDataRequest tire : request.getTires()) {
                if (tire.getTreadDepth() != null && tire.getTreadDepth().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Tire tread depth must be non-negative");
                }
                if (tire.getPressure() != null && tire.getPressure().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Tire pressure must be non-negative");
                }
            }
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
}
