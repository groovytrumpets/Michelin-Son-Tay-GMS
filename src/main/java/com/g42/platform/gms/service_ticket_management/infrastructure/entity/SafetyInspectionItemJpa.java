package com.g42.platform.gms.service_ticket_management.infrastructure.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.ItemStatus;
import jakarta.persistence.*;
import lombok.Data;

/**
 * JPA entity for safety_inspection_item table.
 */
@Entity
@Table(name = "safety_inspection_item",
       uniqueConstraints = @UniqueConstraint(columnNames = {"inspection_id", "work_category_id"}))
@Data
public class SafetyInspectionItemJpa {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;
    
    @Column(name = "inspection_id", nullable = false)
    private Integer inspectionId;
    
    @Column(name = "work_category_id", nullable = false)
    private Integer workCategoryId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", length = 20)
    private ItemStatus itemStatus;
    
    @Column(name = "advisor_note", columnDefinition = "TEXT")
    private String advisorNote;
}
