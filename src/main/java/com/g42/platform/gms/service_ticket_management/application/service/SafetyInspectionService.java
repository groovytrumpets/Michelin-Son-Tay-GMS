package com.g42.platform.gms.service_ticket_management.application.service;


import com.g42.platform.gms.service_ticket_management.api.dto.safety.AdvisorNoteItemRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.AddCustomCategoryRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.InspectionItemRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.InspectionItemResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.SafetyInspectionRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.SafetyInspectionResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.TireDataRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.TireInputRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.WorkCategoryResponse;
import com.g42.platform.gms.service_ticket_management.api.mapper.SafetyInspectionMapper;
import com.g42.platform.gms.service_ticket_management.api.mapper.WorkCategoryApiMapper;
import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspection;
import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspectionItem;
import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspectionTire;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.entity.TicketCustomCategory;
import com.g42.platform.gms.service_ticket_management.domain.entity.WorkCategory;
import com.g42.platform.gms.service_ticket_management.domain.enums.InspectionStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.TirePosition;
import com.g42.platform.gms.service_ticket_management.domain.repository.SafetyInspectionItemRepo;
import com.g42.platform.gms.service_ticket_management.domain.repository.SafetyInspectionRepo;
import com.g42.platform.gms.service_ticket_management.domain.repository.SafetyInspectionTireRepo;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import com.g42.platform.gms.service_ticket_management.domain.repository.TicketCustomCategoryRepo;
import com.g42.platform.gms.service_ticket_management.domain.repository.WorkCategoryRepo;
import com.g42.platform.gms.service_ticket_management.domain.projection.SafetyInspectionItemWithCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class SafetyInspectionService {
    private final SafetyInspectionRepo inspectionRepo;
    private final SafetyInspectionTireRepo tireRepo;
    private final SafetyInspectionItemRepo itemRepo;
    private final WorkCategoryRepo workCategoryRepo;
    private final TicketCustomCategoryRepo customCategoryRepo;
    private final ServiceTicketRepo serviceTicketRepo;
    private final SafetyInspectionMapper apiMapper;
    private final WorkCategoryApiMapper workCategoryApiMapper;
    public SafetyInspectionResponse enableInspectionByCode(String ticketCode, Integer technicianId) {
        ServiceTicket serviceTicket = serviceTicketRepo.findByTicketCode(ticketCode)
                .orElseThrow(() -> new IllegalArgumentException("Service ticket not found with code: " + ticketCode));
        return enableInspection(serviceTicket.getServiceTicketId(), technicianId);
    }


    public SafetyInspectionResponse skipInspectionByCode(String ticketCode) {
        ServiceTicket serviceTicket = serviceTicketRepo.findByTicketCode(ticketCode)
                .orElseThrow(() -> new IllegalArgumentException("Service ticket not found with code: " + ticketCode));
        return skipInspection(serviceTicket.getServiceTicketId());
    }


    public SafetyInspectionResponse reopenInspectionByCode(String ticketCode) {
        ServiceTicket serviceTicket = serviceTicketRepo.findByTicketCode(ticketCode)
                .orElseThrow(() -> new IllegalArgumentException("Service ticket not found with code: " + ticketCode));
        return reopenInspection(serviceTicket.getServiceTicketId());
    }


    @Transactional(readOnly = true)
    public SafetyInspectionResponse getInspectionByTicketCode(String ticketCode) {
        ServiceTicket serviceTicket = serviceTicketRepo.findByTicketCode(ticketCode)
                .orElseThrow(() -> new IllegalArgumentException("Service ticket not found with code: " + ticketCode));
        return getInspectionByServiceTicket(serviceTicket.getServiceTicketId());
    }


    public SafetyInspectionResponse enableInspection(Integer serviceTicketId, Integer technicianId) {
        if (inspectionRepo.findByServiceTicketId(serviceTicketId).isPresent()) {
            throw new IllegalStateException("Inspection already exists for service ticket: " + serviceTicketId);
        }
        if (technicianId == null) {
            throw new IllegalArgumentException("Technician ID is required to enable inspection");
        }
        SafetyInspection domain = new SafetyInspection();
        domain.setServiceTicketId(serviceTicketId);
        domain.setTechnicianId(technicianId);
        domain.setInspectionStatus(InspectionStatus.PENDING);
        domain.initializeDefaults();
        SafetyInspection saved = inspectionRepo.save(domain);
        List<WorkCategory> defaultCategories = workCategoryRepo.findDefaultCategories();
        for (WorkCategory cat : defaultCategories) {
            SafetyInspectionItem item = new SafetyInspectionItem();
            item.setInspectionId(saved.getInspectionId());
            item.setWorkCategoryId(cat.getId());
            itemRepo.save(item);
        }
        ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(serviceTicketId);
        if (ticket == null) throw new IllegalArgumentException("Service ticket not found: " + serviceTicketId);
        ticket.setSafetyInspectionEnabled(true);
        ticket.setTicketStatus(TicketStatus.DRAFT);
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);
        return getInspectionByServiceTicket(serviceTicketId);
    }


    public SafetyInspectionResponse skipInspection(Integer serviceTicketId) {
        if (inspectionRepo.findByServiceTicketId(serviceTicketId).isPresent()) {
            throw new IllegalStateException("Inspection already exists for service ticket: " + serviceTicketId);
        }
        SafetyInspection domain = new SafetyInspection();
        domain.setServiceTicketId(serviceTicketId);
        domain.setTechnicianId(null);
        domain.setInspectionStatus(InspectionStatus.SKIPPED);
        domain.initializeDefaults();
        SafetyInspection saved = inspectionRepo.save(domain);

        // Update ServiceTicket to set safetyInspectionEnabled = false
        ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(serviceTicketId);
        if (ticket == null) throw new IllegalArgumentException("Service ticket not found: " + serviceTicketId);
        ticket.setSafetyInspectionEnabled(false);
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);

        return apiMapper.toResponse(saved);
    }


    public SafetyInspectionResponse reopenInspection(Integer serviceTicketId) {
        SafetyInspection inspection = inspectionRepo.findByServiceTicketId(serviceTicketId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found for service ticket: " + serviceTicketId));
        inspection.setInspectionStatus(InspectionStatus.PENDING);
        inspection.setUpdatedAt(LocalDateTime.now());
        inspectionRepo.save(inspection);

        ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(serviceTicketId);
        if (ticket == null) throw new IllegalArgumentException("Service ticket not found: " + serviceTicketId);
        ticket.setTicketStatus(TicketStatus.DRAFT);
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);

        return getInspectionByServiceTicket(serviceTicketId);
    }


    /** Cho phép chỉnh phiếu kiểm tra khi ticket ở DRAFT hoặc INSPECTION */
    private void requireTicketEditableForInspection(Integer serviceTicketId) {
        ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(serviceTicketId);
        if (ticket == null) throw new IllegalArgumentException("Service ticket not found: " + serviceTicketId);
        TicketStatus status = ticket.getTicketStatus();
        if (status != TicketStatus.DRAFT && status != TicketStatus.INSPECTION) {
            throw new IllegalStateException(
                    "Chi co the chinh sua phieu kiem tra khi ticket o DRAFT hoac INSPECTION. " +
                            "Trang thai hien tai: " + status);
        }
    }


    public SafetyInspectionResponse saveInspectionData(SafetyInspectionRequest request, Integer technicianId) {
        requireTicketEditableForInspection(request.getServiceTicketId());
        validateInspectionData(request);
        SafetyInspection domain = apiMapper.toDomain(request);
        domain.setTechnicianId(technicianId);
        domain.setInspectionStatus(InspectionStatus.COMPLETED);
        Optional<SafetyInspection> existing = inspectionRepo.findByServiceTicketId(request.getServiceTicketId());
        SafetyInspection saved;
        if (existing.isPresent()) {
            SafetyInspection e = existing.get();
            e.setGeneralNotes(request.getGeneralNotes());
            e.setTechnicianNotes(request.getTechnicianNotes());
            e.setTechnicianId(technicianId);
            e.setInspectionStatus(InspectionStatus.COMPLETED);
            e.setUpdatedAt(LocalDateTime.now());
            saved = inspectionRepo.save(e);
        } else {
            domain.initializeDefaults();
            saved = inspectionRepo.save(domain);
        }
        updateTires(saved.getInspectionId(), expandTires(request.getTires()));
        updateItems(saved.getInspectionId(), request.getItems());
        // Technician submit xong → ticket về DRAFT
        ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(request.getServiceTicketId());
        if (ticket != null && ticket.getTicketStatus() == TicketStatus.INSPECTION) {
            ticket.setTicketStatus(TicketStatus.DRAFT);
            ticket.setUpdatedAt(LocalDateTime.now());
            serviceTicketRepo.save(ticket);
        }
        return getInspectionByServiceTicket(request.getServiceTicketId());
    }


    @Transactional(readOnly = true)
    public SafetyInspectionResponse getInspectionById(Integer inspectionId) {
        SafetyInspection domain = inspectionRepo.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found with ID: " + inspectionId));
        domain.setTires(tireRepo.findByInspectionId(inspectionId));
        domain.setItems(itemRepo.findItemsWithCategory(inspectionId));
        return apiMapper.toResponse(domain);
    }


    @Transactional(readOnly = true)
    public SafetyInspectionResponse getInspectionByServiceTicket(Integer serviceTicketId) {
        SafetyInspection domain = inspectionRepo.findByServiceTicketId(serviceTicketId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found for service ticket: " + serviceTicketId));
        domain.setTires(tireRepo.findByInspectionId(domain.getInspectionId()));
        domain.setItems(itemRepo.findItemsWithCategory(domain.getInspectionId()));
        return apiMapper.toResponse(domain);
    }


    public SafetyInspectionResponse updateInspectionData(Integer inspectionId, SafetyInspectionRequest request) {
        validateInspectionData(request);
        SafetyInspection inspection = inspectionRepo.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));
        inspection.setGeneralNotes(request.getGeneralNotes());
        inspection.setTechnicianNotes(request.getTechnicianNotes());
        inspection.setInspectionStatus(InspectionStatus.COMPLETED);
        inspection.setUpdatedAt(LocalDateTime.now());
        SafetyInspection saved = inspectionRepo.save(inspection);
        updateTires(inspectionId, expandTires(request.getTires()));
        updateItems(inspectionId, request.getItems());
        return getInspectionByServiceTicket(saved.getServiceTicketId());
    }
    private void updateTires(Integer inspectionId, List<TireDataRequest> requestTires) {
        List<SafetyInspectionTire> existingTires = tireRepo.findByInspectionId(inspectionId);
        if (requestTires == null || requestTires.isEmpty()) { tireRepo.deleteAll(existingTires); return; }
        List<SafetyInspectionTire> newTires = apiMapper.tiresToDomain(requestTires);
        Map<TirePosition, SafetyInspectionTire> existingMap = existingTires.stream()
                .collect(Collectors.toMap(SafetyInspectionTire::getTirePosition, t -> t, (a, b) -> a));
        Set<TirePosition> requested = new HashSet<>();
        for (SafetyInspectionTire nt : newTires) {
            nt.setInspectionId(inspectionId);
            requested.add(nt.getTirePosition());
            SafetyInspectionTire ex = existingMap.get(nt.getTirePosition());
            if (ex != null) {
                ex.setTreadDepth(nt.getTreadDepth()); ex.setPressure(nt.getPressure());
                ex.setPressureUnit(nt.getPressureUnit()); ex.setTireSpecification(nt.getTireSpecification());
                ex.setRecommendedTireSize(nt.getRecommendedTireSize());
                ex.setRecommendedPressure(nt.getRecommendedPressure());
                ex.setRecommendedPressureUnit(nt.getRecommendedPressureUnit());
                tireRepo.save(ex);
            } else { tireRepo.save(nt); }
        }
        for (SafetyInspectionTire et : existingTires) {
            if (!requested.contains(et.getTirePosition())) tireRepo.delete(et);
        }
    }


    private void updateItems(Integer inspectionId, List<InspectionItemRequest> requestItems) {
        List<SafetyInspectionItem> existingItems = itemRepo.findByInspectionId(inspectionId);
        if (requestItems == null || requestItems.isEmpty()) { itemRepo.deleteAll(existingItems); return; }
        List<SafetyInspectionItem> newItems = apiMapper.itemsToDomain(requestItems);
        Map<Integer, SafetyInspectionItem> byWork = existingItems.stream()
                .filter(i -> i.getWorkCategoryId() != null)
                .collect(Collectors.toMap(SafetyInspectionItem::getWorkCategoryId, i -> i, (a, b) -> a));
        Map<Integer, SafetyInspectionItem> byCustom = existingItems.stream()
                .filter(i -> i.getCustomCategoryId() != null)
                .collect(Collectors.toMap(SafetyInspectionItem::getCustomCategoryId, i -> i, (a, b) -> a));
        Set<Integer> touched = new HashSet<>();
        for (SafetyInspectionItem ni : newItems) {
            ni.setInspectionId(inspectionId);
            SafetyInspectionItem ex = ni.getWorkCategoryId() != null ? byWork.get(ni.getWorkCategoryId())
                    : byCustom.get(ni.getCustomCategoryId());
            if (ex != null) { ex.setItemStatus(ni.getItemStatus()); touched.add(itemRepo.save(ex).getItemId()); }
            else { touched.add(itemRepo.save(ni).getItemId()); }
        }
        for (SafetyInspectionItem ei : existingItems) {
            if (!touched.contains(ei.getItemId()) && ei.getCustomCategoryId() != null) itemRepo.delete(ei);
        }
    }


    @Transactional(readOnly = true)
    public List<InspectionItemResponse> getInspectionItems(Integer inspectionId) {
        return itemRepo.findByInspectionIdWithCategory(inspectionId).stream().map(p -> {
            InspectionItemResponse r = new InspectionItemResponse();
            r.setItemId(p.getItemId()); r.setWorkCategoryId(p.getWorkCategoryId());
            r.setCustomCategoryId(p.getCustomCategoryId()); r.setCategoryName(p.getCategoryName());
            r.setItemStatus(p.getItemStatus()); r.setAdvisorNote(p.getAdvisorNote());
            return r;
        }).toList();
    }


    @Transactional
    public List<InspectionItemResponse> upsertItems(Integer inspectionId, List<InspectionItemRequest> requests) {
        SafetyInspection inspection = inspectionRepo.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));
        requireTicketEditableForInspection(inspection.getServiceTicketId());
        return requests.stream().map(req -> upsertItemInternal(inspectionId, req)).toList();
    }


    private InspectionItemResponse upsertItemInternal(Integer inspectionId, InspectionItemRequest request) {
        if (request.getWorkCategoryId() == null && request.getCustomCategoryId() == null)
            throw new IllegalArgumentException("workCategoryId hoac customCategoryId la bat buoc");
        SafetyInspectionItem item;
        if (request.getWorkCategoryId() != null) {
            item = itemRepo.findByInspectionIdAndWorkCategoryId(inspectionId, request.getWorkCategoryId())
                    .orElseGet(() -> { SafetyInspectionItem n = new SafetyInspectionItem();
                        n.setInspectionId(inspectionId); n.setWorkCategoryId(request.getWorkCategoryId()); return n; });
        } else {
            item = itemRepo.findByInspectionIdAndCustomCategoryId(inspectionId, request.getCustomCategoryId())
                    .orElseGet(() -> { SafetyInspectionItem n = new SafetyInspectionItem();
                        n.setInspectionId(inspectionId); n.setCustomCategoryId(request.getCustomCategoryId()); return n; });
        }
        item.setItemStatus(request.getItemStatus());
        return buildItemResponse(itemRepo.save(item), inspectionId);
    }


    @Transactional(readOnly = true)
    public List<WorkCategoryResponse> getDefaultSafetyInspectionCategories() {
        List<WorkCategory> workCategories = workCategoryRepo.findDefaultCategories();
        return workCategoryApiMapper.toResponseList(workCategories);
    }


    public List<InspectionItemResponse> updateAdvisorNotes(Integer inspectionId, List<AdvisorNoteItemRequest> noteItems) {
        return noteItems.stream().map(ni -> {
            if (ni.getWorkCategoryId() == null && ni.getCustomCategoryId() == null)
                throw new IllegalArgumentException("workCategoryId hoac customCategoryId la bat buoc");
            SafetyInspectionItem item;
            if (ni.getWorkCategoryId() != null) {
                item = itemRepo.findByInspectionIdAndWorkCategoryId(inspectionId, ni.getWorkCategoryId())
                        .orElseGet(() -> { SafetyInspectionItem n = new SafetyInspectionItem();
                            n.setInspectionId(inspectionId); n.setWorkCategoryId(ni.getWorkCategoryId()); return n; });
            } else {
                item = itemRepo.findByInspectionIdAndCustomCategoryId(inspectionId, ni.getCustomCategoryId())
                        .orElseGet(() -> { SafetyInspectionItem n = new SafetyInspectionItem();
                            n.setInspectionId(inspectionId); n.setCustomCategoryId(ni.getCustomCategoryId()); return n; });
            }
            item.setAdvisorNote(ni.getAdvisorNote());
            return buildItemResponse(itemRepo.save(item), inspectionId);
        }).toList();
    }


    @Transactional
    public InspectionItemResponse addCustomCategory(Integer inspectionId, AddCustomCategoryRequest request) {
        SafetyInspection inspection = inspectionRepo.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay phieu kiem tra: " + inspectionId));
        requireTicketEditableForInspection(inspection.getServiceTicketId());
        if (customCategoryRepo.existsByInspectionIdAndCategoryName(inspectionId, request.getCategoryName()))
            throw new IllegalArgumentException("Hang muc da ton tai trong phieu nay");
        TicketCustomCategory cat = new TicketCustomCategory();
        cat.setInspectionId(inspectionId); cat.setCategoryName(request.getCategoryName());
        cat.setDisplayOrder(request.getDisplayOrder());
        TicketCustomCategory savedCat = customCategoryRepo.save(cat);
        SafetyInspectionItem item = new SafetyInspectionItem();
        item.setInspectionId(inspectionId); item.setCustomCategoryId(savedCat.getId());
        SafetyInspectionItem savedItem = itemRepo.save(item);
        InspectionItemResponse resp = new InspectionItemResponse();
        resp.setItemId(savedItem.getItemId()); resp.setCustomCategoryId(savedCat.getId());
        resp.setCategoryName(savedCat.getCategoryName());
        return resp;
    }

    @Transactional
    public void deleteCustomCategory(Integer inspectionId, Integer categoryId) {
        SafetyInspection inspection = inspectionRepo.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu kiểm tra: " + inspectionId));
        requireTicketEditableForInspection(inspection.getServiceTicketId());

        TicketCustomCategory cat = customCategoryRepo.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hạng mục: " + categoryId));

        if (!cat.getInspectionId().equals(inspectionId)) {
            throw new IllegalArgumentException("Hạng mục không thuộc phiếu kiểm tra này");
        }

        cat.setStatus("DELETED");
        customCategoryRepo.save(cat);
    }


    private InspectionItemResponse buildItemResponse(SafetyInspectionItem saved, Integer inspectionId) {
        List<SafetyInspectionItemWithCategory> withCat = itemRepo.findByInspectionIdWithCategory(inspectionId);
        return withCat.stream().filter(p -> p.getItemId().equals(saved.getItemId())).findFirst()
                .map(p -> { InspectionItemResponse r = new InspectionItemResponse();
                    r.setItemId(p.getItemId()); r.setWorkCategoryId(p.getWorkCategoryId());
                    r.setCustomCategoryId(p.getCustomCategoryId()); r.setCategoryName(p.getCategoryName());
                    r.setItemStatus(saved.getItemStatus()); r.setAdvisorNote(saved.getAdvisorNote()); return r; })
                .orElseGet(() -> { InspectionItemResponse r = new InspectionItemResponse();
                    r.setItemId(saved.getItemId()); r.setWorkCategoryId(saved.getWorkCategoryId());
                    r.setCustomCategoryId(saved.getCustomCategoryId());
                    r.setItemStatus(saved.getItemStatus()); r.setAdvisorNote(saved.getAdvisorNote()); return r; });
    }


    private void validateInspectionData(SafetyInspectionRequest request) {
        if (request.getServiceTicketId() == null) throw new IllegalArgumentException("Service ticket ID is required");
        if (request.getTires() != null) {
            TireInputRequest t = request.getTires();
            validateActualTire("frontLeft", t.getFrontLeft()); validateActualTire("frontRight", t.getFrontRight());
            validateActualTire("rearLeft", t.getRearLeft()); validateActualTire("rearRight", t.getRearRight());
            validateActualTire("spare", t.getSpare());
        }
        if (request.getItems() != null) {
            for (InspectionItemRequest item : request.getItems()) {
                if (item.getWorkCategoryId() == null && item.getCustomCategoryId() == null)
                    throw new IllegalArgumentException("workCategoryId hoac customCategoryId la bat buoc");
            }
        }
    }


    private void validateActualTire(String label, TireInputRequest.TireActualData tire) {
        if (tire == null) return;
        if (tire.getTreadDepth() != null && tire.getTreadDepth().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException(label + " tread depth must be non-negative");
        if (tire.getPressure() != null && tire.getPressure().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException(label + " pressure must be non-negative");
    }


    private List<TireDataRequest> expandTires(TireInputRequest input) {
        if (input == null) return List.of();
        String fs = input.getFrontTireSpecification(), rs = input.getRearTireSpecification();
        String rec = input.getRecommendedTireSize();
        BigDecimal fr = input.getFrontRecommendedPressure(), rr = input.getRearRecommendedPressure();
        BigDecimal sr = input.getSpareRecommendedPressure();
        return List.of(
                buildTire(TirePosition.FRONT_LEFT, input.getFrontLeft(), fs, rec, fr),
                buildTire(TirePosition.FRONT_RIGHT, input.getFrontRight(), fs, rec, fr),
                buildTire(TirePosition.REAR_LEFT, input.getRearLeft(), rs, rec, rr),
                buildTire(TirePosition.REAR_RIGHT, input.getRearRight(), rs, rec, rr),
                buildTire(TirePosition.SPARE, input.getSpare(), null, null, sr));
    }


    private TireDataRequest buildTire(TirePosition pos, TireInputRequest.TireActualData actual,
                                      String spec, String recSize, BigDecimal recPressure) {
        TireDataRequest req = new TireDataRequest();
        req.setTirePosition(pos);
        if (actual != null) { req.setTreadDepth(actual.getTreadDepth()); req.setPressure(actual.getPressure());
            req.setPressureUnit(actual.getPressureUnit()); }
        req.setTireSpecification(spec); req.setRecommendedTireSize(recSize); req.setRecommendedPressure(recPressure);
        return req;
    }

    public String saveRecommend(Integer serviceTicketId, String recommend) {
        SafetyInspection safetyInspection = inspectionRepo.findByIdService(serviceTicketId);
        safetyInspection.setGeneralNotes(recommend);
        SafetyInspection saved = inspectionRepo.save(safetyInspection);
        return saved.getGeneralNotes();

    }

    public SafetyInspection findByServiceTicketId(Integer previousId) {
        return inspectionRepo.findByIdService(previousId);
    }
}

