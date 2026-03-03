package com.g42.platform.gms.booking_management.domain.entity;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking_management.domain.exception.BookingStaffErrorCode;
import com.g42.platform.gms.booking_management.domain.exception.BookingStaffException;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class BookingRequest {
    private Integer requestId;
    private String phone;
    private String fullName;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String description;
    private String serviceCategory;
    private BookingRequestStatus status = BookingRequestStatus.PENDING;
    private Boolean isGuest = true;
    private CustomerProfile customer;
    private StaffProfile confirmedBy;
    private LocalDateTime confirmedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String clientIp;
    private List<CatalogItem> services;
    private String requestCode;
    private String note;

    public boolean isGuest() {
        return isGuest;
    }
    public boolean isPending() {
        return status == BookingRequestStatus.PENDING;
    }

    public boolean confirm() {
        if (this.status != BookingRequestStatus.PENDING) {
            throw new BookingStaffException("Không thể confirm booking!", BookingStaffErrorCode.BOOKING_STATUS_WRONG);
        }
        this.status = BookingRequestStatus.CONFIRMED;
        return true;
    }

    public void cancel(String reason, String userNote) {

        if (cantCancel()) {
            throw new BookingStaffException("Booking này đã xử lý rồi!", BookingStaffErrorCode.BOOKING_CANT_CANCEL);
        }

        this.status = BookingRequestStatus.REJECTED;

        String finalNote = buildCancelMessage(reason, userNote);

        this.note = finalNote;
    }
    public void spam(String reason, String userNote) {

        if (cantCancel()) {
            throw new BookingStaffException("Booking này đã xử lý rồi!", BookingStaffErrorCode.BOOKING_CANT_CANCEL);
        }

        this.status = BookingRequestStatus.SPAM;

        this.note = buildCancelMessage(reason, userNote);
    }

    public void contacted(String reason, String userNote) {

        if (cantCancel()) {
            throw new BookingStaffException("Booking này đã xử lý rồi!", BookingStaffErrorCode.BOOKING_CANT_CANCEL);
        }

        this.status = BookingRequestStatus.CONTACTED;

        this.note = buildCancelMessage(reason, userNote);
    }

    public boolean cantCancel() {
        return this.status != BookingRequestStatus.PENDING && this.status != BookingRequestStatus.CONFIRMED;
    }

    private String buildCancelMessage(String reason, String userNote) {

        StringBuilder sb = new StringBuilder();

        if (reason != null && !reason.isBlank()) {
            sb.append("Nội dung: ");
            sb.append(reason).append("\n");
        }

        if (userNote != null && !userNote.isBlank()) {
            sb.append(userNote);
        }

        return sb.toString();
    }
}
