package com.g42.platform.gms.service_ticket_management.infrastructure.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.PhotoCategory;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * JPA entity for vehicle_condition_photo table.
 * 
 * Maps to the vehicle_condition_photo table in the database.
 * This entity stores photos of vehicle condition taken during check-in,
 * including standard condition photos and DAMAGE photos with descriptions.
 */
@Entity
@Table(name = "vehicle_condition_photo", indexes = {
    @Index(name = "idx_service_ticket", columnList = "service_ticket_id"),
    @Index(name = "idx_category", columnList = "category")
})
@Data
public class VehicleConditionPhotoJpa {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id")
    private Integer photoId;
    
    @Column(name = "service_ticket_id", nullable = false)
    private Integer serviceTicketId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private PhotoCategory category;
    
    @Column(name = "photo_url", nullable = false, length = 500)
    private String photoUrl;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
    
    @Column(name = "uploaded_by", nullable = false)
    private Integer uploadedBy;
    
    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
