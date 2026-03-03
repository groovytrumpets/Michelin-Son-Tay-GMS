package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO after completing check-in.
 * Contains service ticket information and any warnings.
 */
@Data
public class ServiceTicketResponse {
    
    private Integer serviceTicketId;
    private String ticketCode;
    private Integer bookingId;
    private Integer vehicleId;
    private TicketStatus ticketStatus;
    private Integer odometerReading;
    private String licensePlatePhotoUrl;
    private String checkInNotes;
    private Boolean immutable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Photo information
    private List<PhotoInfo> photos = new ArrayList<>();
    
    // Warnings (if any)
    private List<Warning> warnings = new ArrayList<>();
    
    @Data
    public static class PhotoInfo {
        private Integer photoId;
        private String category;
        private String photoUrl;
        private String description;
    }
    
    @Data
    public static class Warning {
        private String code;
        private String message;
        private String severity;  // INFO, WARNING, ERROR
    }
}
