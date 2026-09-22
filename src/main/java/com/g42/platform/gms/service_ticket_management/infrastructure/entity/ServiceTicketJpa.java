package com.g42.platform.gms.service_ticket_management.infrastructure.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.BackfillKind;
import com.g42.platform.gms.service_ticket_management.domain.enums.BackfillReviewStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.EntryMode;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for service_ticket table.
 * 
 * Maps to the service_ticket table in the database.
 * This entity represents a service ticket created during vehicle check-in.
 */
@Entity(name = "ServiceTicketManagement")
@Table(name = "service_ticket")
@Data
public class ServiceTicketJpa {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_ticket_id")
    private Integer serviceTicketId;
    
    @Column(name = "ticket_code", length = 50, unique = true)
    private String ticketCode;
    
    @Column(name = "booking_id")
    private Integer bookingId;
    
    // Nullable: phiếu bán linh kiện (PARTS_SALE) không gắn xe
    @Column(name = "vehicle_id")
    private Integer vehicleId;
    
    @Column(name = "customer_id", nullable = false)
    private Integer customerId;
    
    @Column(name = "created_by")
    private Integer createdBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_status", length = 50)
    private TicketStatus ticketStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", length = 20)
    private TicketType ticketType = TicketType.SERVICE;
    
    @Column(name = "received_at")
    private LocalDateTime receivedAt;
    
    @Column(name = "immutable")
    private Boolean immutable = false;
    
    @Column(name = "customer_request", columnDefinition = "TINYTEXT")
    private String customerRequest;
    
    @Column(name = "technician_notes", columnDefinition = "TINYTEXT")
    private String technicianNotes;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "estimated_delivery_at")
    private LocalDateTime estimatedDeliveryAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    @Column(name = "check_in_notes", columnDefinition = "TEXT")
    private String checkInNotes;

    @Column(name = "safety_inspection_enabled")
    private Boolean safetyInspectionEnabled = false;

    @Column(name = "is_printed")
    private Boolean isPrinted = false;

    @Column(name = "printed_at")
    private LocalDateTime printedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "serviceTicketId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VehicleConditionPhotoJpa> conditionPhotos = new ArrayList<>();
    
    @OneToMany(mappedBy = "serviceTicketId", fetch = FetchType.LAZY)
    private List<ServiceTicketAssignmentJpa> assignments = new ArrayList<>();
    @Column(name = "queue_number")
    private Integer queueNumber;

    // ===== Nhập bù phiếu ngày trước (Liquibase 036) =====
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_mode", length = 20, nullable = false)
    private EntryMode entryMode = EntryMode.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "backfill_kind", length = 20)
    private BackfillKind backfillKind;

    @Column(name = "backfill_parent_ticket_id")
    private Integer backfillParentTicketId;

    @Column(name = "backfill_reason", length = 500)
    private String backfillReason;

    @Column(name = "backfill_payment_method", length = 20)
    private String backfillPaymentMethod;

    @Column(name = "backfill_advisor_id")
    private Integer backfillAdvisorId;

    @Column(name = "backfill_technician_id")
    private Integer backfillTechnicianId;

    @Enumerated(EnumType.STRING)
    @Column(name = "backfill_review_status", length = 20)
    private BackfillReviewStatus backfillReviewStatus;

    @Column(name = "backfill_reviewed_by")
    private Integer backfillReviewedBy;

    @Column(name = "backfill_reviewed_at")
    private LocalDateTime backfillReviewedAt;

    @Column(name = "backfill_review_note", length = 500)
    private String backfillReviewNote;

    // ===== Bán cho khách lẻ vãng lai (Liquibase 042) =====
    // customer_id trỏ về hồ sơ dùng chung "Khách lẻ"; tên/SĐT/địa chỉ thật của lượt
    // bán nằm ở đây nên mỗi phiếu vẫn giữ được người mua riêng của nó.
    @Column(name = "is_walk_in", nullable = false)
    private Boolean isWalkIn = false;

    @Column(name = "walk_in_name")
    private String walkInName;

    @Column(name = "walk_in_phone", length = 30)
    private String walkInPhone;

    @Column(name = "walk_in_address", length = 500)
    private String walkInAddress;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        ensureEntryMode();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        ensureEntryMode();
    }

    // Domain → JPA (MapStruct) copy cả giá trị null: các luồng cũ không biết tới
    // entryMode sẽ ghi NULL vào cột NOT NULL nếu không chặn ở đây.
    private void ensureEntryMode() {
        if (entryMode == null) {
            entryMode = EntryMode.NORMAL;
        }
    }
}
