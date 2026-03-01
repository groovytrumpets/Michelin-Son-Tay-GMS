package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import com.g42.platform.gms.service_ticket_management.domain.enums.PhotoCategory;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for single-page check-in form.
 * Contains all information needed to complete check-in in one API call.
 */
@Data
public class CompleteCheckInAllRequest {
    
    // Booking information
    private Integer bookingId;
    private Integer customerId;
    
    // Vehicle information (either existing or new)
    private Integer vehicleId;              // If selecting existing vehicle
    private String licensePlate;            // If creating new vehicle
    private String make;                    // If creating new vehicle
    private String model;                   // If creating new vehicle
    private Integer year;                   // If creating new vehicle
    
    // License plate photo (optional)
    private MultipartFile licensePlatePhoto;
    
    // Vehicle condition photos (at least 1 required)
    private MultipartFile photoFront;
    private String photoFrontDescription;
    
    private MultipartFile photoRear;
    private String photoRearDescription;
    
    private MultipartFile photoLeftSide;
    private String photoLeftSideDescription;
    
    private MultipartFile photoRightSide;
    private String photoRightSideDescription;
    
    private MultipartFile photoInterior;
    private String photoInteriorDescription;
    
    private MultipartFile photoDamage;      // Optional
    private String photoDamageDescription;
    
    // Odometer reading (required)
    private Integer odometerReading;
    
    // Check-in notes (optional)
    private String checkInNotes;
    
    // Staff ID who performs check-in (receptionist/staff)
    // This will be used for both uploadedBy (photos) and recordedBy (odometer)
    private Integer staffId;
}
