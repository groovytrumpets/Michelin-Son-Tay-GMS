package com.g42.platform.gms.attendancerequest.application.service;

import com.g42.platform.gms.attendancerequest.api.dto.AttendanceRequestCreateRequest;
import com.g42.platform.gms.attendancerequest.api.dto.AttendanceRequestResponse;
import com.g42.platform.gms.attendancerequest.api.mapper.AttendanceRequestDtoMapper;
import com.g42.platform.gms.attendancerequest.domain.entity.AttendanceRequest;
import com.g42.platform.gms.attendancerequest.domain.exception.AttendanceRequestErrorCode;
import com.g42.platform.gms.attendancerequest.domain.exception.AttendanceRequestException;
import com.g42.platform.gms.attendancerequest.domain.repository.AttendanceRequestRepo;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.dashboard.application.service.StaffNotifyService;
import com.g42.platform.gms.manager.attendance.domain.entity.AttendanceCheckin;
import com.g42.platform.gms.manager.attendance.domain.repository.AttendanceCheckinRepo;
import com.g42.platform.gms.manager.schedule.domain.entity.WorkShift;
import com.g42.platform.gms.manager.schedule.domain.repository.WorkShiftRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceRequestService {

    private static final String TYPE_COMPENSATORY = "COMPENSATORY";
    private static final String TYPE_LEAVE = "LEAVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String MANAGER_INBOX_URL = "/attendance-request-management";
    private static final String STAFF_REQUESTS_URL = "/attendance-requests";

    private final AttendanceRequestRepo requestRepo;
    private final AttendanceRequestDtoMapper dtoMapper;
    private final AttendanceCheckinRepo checkinRepo;
    private final StaffProfileRepo staffProfileRepo;
    private final WorkShiftRepo workShiftRepo;
    private final StaffNotifyService staffNotifyService;

    public List<AttendanceRequestResponse> listMine(Integer staffId) {
        return requestRepo.findByStaffId(staffId).stream().map(this::enrichAndMap).toList();
    }

    public List<AttendanceRequestResponse> listForManager(String status, String requestType) {
        return requestRepo.findByFilters(status, requestType).stream().map(this::enrichAndMap).toList();
    }

    @Transactional
    public AttendanceRequestResponse create(Integer staffId, AttendanceRequestCreateRequest req) {
        String type = String.valueOf(req.getRequestType()).toUpperCase();
        if (!TYPE_COMPENSATORY.equals(type) && !TYPE_LEAVE.equals(type)) {
            throw new AttendanceRequestException(AttendanceRequestErrorCode.INVALID_REQUEST_TYPE);
        }
        if (!StringUtils.hasText(req.getReason())) {
            throw new AttendanceRequestException(AttendanceRequestErrorCode.REASON_REQUIRED);
        }

        LocalDate startDate = req.getStartDate();
        LocalDate endDate = TYPE_LEAVE.equals(type) && req.getEndDate() != null ? req.getEndDate() : startDate;
        if (endDate.isBefore(startDate)) {
            throw new AttendanceRequestException(AttendanceRequestErrorCode.INVALID_DATE_RANGE);
        }

        AttendanceRequest request = new AttendanceRequest();
        request.setStaffId(staffId);
        request.setRequestType(type);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setShiftId(TYPE_COMPENSATORY.equals(type) ? req.getShiftId() : null);
        request.setCheckInTime(TYPE_COMPENSATORY.equals(type) ? req.getCheckInTime() : null);
        request.setCheckOutTime(TYPE_COMPENSATORY.equals(type) ? req.getCheckOutTime() : null);
        request.setReason(req.getReason());
        request.setStatus(STATUS_PENDING);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        AttendanceRequest saved = requestRepo.save(request);

        notifyManagers(saved, staffId);

        return enrichAndMap(saved);
    }

    @Transactional
    public void cancel(Integer staffId, Integer requestId) {
        AttendanceRequest request = findRequest(requestId);
        if (!staffId.equals(request.getStaffId())) {
            throw new AttendanceRequestException(AttendanceRequestErrorCode.NOT_OWNER);
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new AttendanceRequestException(AttendanceRequestErrorCode.NOT_PENDING);
        }
        requestRepo.deleteById(requestId);
    }

    @Transactional
    public AttendanceRequestResponse approve(Integer reviewerStaffId, Integer requestId, String reviewNote) {
        AttendanceRequest request = findRequest(requestId);
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new AttendanceRequestException(AttendanceRequestErrorCode.NOT_PENDING);
        }

        request.setStatus(STATUS_APPROVED);
        request.setReviewedBy(reviewerStaffId);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNote(reviewNote);
        request.setUpdatedAt(LocalDateTime.now());
        AttendanceRequest saved = requestRepo.save(request);

        applyApprovedRequestToAttendance(saved);
        notifyRequester(saved, "Yêu cầu của bạn đã được duyệt",
                requestSummaryLabel(saved) + " đã được quản lý duyệt.", reviewerStaffId);

        return enrichAndMap(saved);
    }

    @Transactional
    public AttendanceRequestResponse reject(Integer reviewerStaffId, Integer requestId, String reviewNote) {
        if (!StringUtils.hasText(reviewNote)) {
            throw new AttendanceRequestException(AttendanceRequestErrorCode.REVIEW_NOTE_REQUIRED);
        }
        AttendanceRequest request = findRequest(requestId);
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new AttendanceRequestException(AttendanceRequestErrorCode.NOT_PENDING);
        }

        request.setStatus(STATUS_REJECTED);
        request.setReviewedBy(reviewerStaffId);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNote(reviewNote);
        request.setUpdatedAt(LocalDateTime.now());
        AttendanceRequest saved = requestRepo.save(request);

        notifyRequester(saved, "Yêu cầu của bạn bị từ chối",
                requestSummaryLabel(saved) + " đã bị từ chối. Lý do: " + reviewNote, reviewerStaffId);

        return enrichAndMap(saved);
    }

    private void applyApprovedRequestToAttendance(AttendanceRequest request) {
        if (TYPE_COMPENSATORY.equals(request.getRequestType())) {
            upsertCheckin(request.getStaffId(), request.getStartDate(), request.getShiftId(),
                    request.getCheckInTime(), request.getCheckOutTime(), "PRESENT", "COMPENSATED");
            return;
        }

        LocalDate cursor = request.getStartDate();
        while (!cursor.isAfter(request.getEndDate())) {
            upsertCheckin(request.getStaffId(), cursor, null, null, null, "LEAVE", "LEAVE_APPROVED");
            cursor = cursor.plusDays(1);
        }
    }

    private void upsertCheckin(Integer staffId, LocalDate date, Integer shiftId,
                                 java.time.LocalTime checkInTime, java.time.LocalTime checkOutTime,
                                 String status, String checkInMethod) {
        List<AttendanceCheckin> existing = checkinRepo.findByStaffAndDate(staffId, date);
        AttendanceCheckin checkin = existing.isEmpty() ? new AttendanceCheckin() : existing.get(0);
        checkin.setStaffId(staffId);
        checkin.setAttendanceDate(date);
        if (shiftId != null) checkin.setShiftId(shiftId);
        checkin.setCheckInTime(checkInTime);
        checkin.setCheckOutTime(checkOutTime);
        checkin.setStatus(status);
        checkin.setCheckInMethod(checkInMethod);
        if (checkin.getCreatedAt() == null) checkin.setCreatedAt(LocalDateTime.now());
        checkinRepo.save(checkin);
    }

    private void notifyManagers(AttendanceRequest request, Integer requesterStaffId) {
        try {
            List<StaffProfile> managers = staffProfileRepo.findByRoleCode("MANAGER");
            List<StaffProfile> admins = staffProfileRepo.findByRoleCode("ADMIN");
            String title = "Yêu cầu mới: " + requestTypeLabel(request.getRequestType());
            String message = requestSummaryLabel(request) + " đang chờ bạn duyệt.";

            // Một nhân viên có thể mang cả 2 role (MANAGER + ADMIN) hoặc bị trùng dòng
            // gán role trong staff_role — gộp và loại trùng theo staffId để chỉ báo 1 lần.
            java.util.Map<Integer, StaffProfile> recipients = new java.util.LinkedHashMap<>();
            java.util.stream.Stream.concat(managers.stream(), admins.stream())
                    .filter(p -> p != null && p.getStaffId() != null)
                    .forEach(p -> recipients.putIfAbsent(p.getStaffId(), p));

            recipients.values().forEach(p -> sendNotificationSafely(p, title, message, requesterStaffId));
        } catch (Exception ignored) {
            // Không để lỗi gửi thông báo làm hỏng giao dịch tạo yêu cầu
        }
    }

    private void sendNotificationSafely(StaffProfile profile, String title, String message, Integer sendBy) {
        if (profile == null || profile.getStaffId() == null) return;
        try {
            staffNotifyService.createNotificationAssignAuto(profile.getStaffId(), title, message, sendBy, MANAGER_INBOX_URL);
        } catch (Exception ignored) {
            // bỏ qua lỗi gửi thông báo cho từng người
        }
    }

    private void notifyRequester(AttendanceRequest request, String title, String message, Integer sendBy) {
        try {
            staffNotifyService.createNotificationAssignAuto(request.getStaffId(), title, message, sendBy, STAFF_REQUESTS_URL);
        } catch (Exception ignored) {
            // Không để lỗi gửi thông báo làm hỏng giao dịch duyệt/từ chối
        }
    }

    private String requestTypeLabel(String type) {
        return TYPE_COMPENSATORY.equals(type) ? "Chấm công bù" : "Đơn xin nghỉ";
    }

    private String requestSummaryLabel(AttendanceRequest request) {
        return requestTypeLabel(request.getRequestType()) + " ngày " + request.getStartDate()
                + (request.getEndDate().equals(request.getStartDate()) ? "" : " - " + request.getEndDate());
    }

    private AttendanceRequest findRequest(Integer requestId) {
        return requestRepo.findById(requestId)
                .orElseThrow(() -> new AttendanceRequestException(AttendanceRequestErrorCode.REQUEST_NOT_FOUND));
    }

    private AttendanceRequestResponse enrichAndMap(AttendanceRequest request) {
        Optional.ofNullable(request.getStaffId())
                .flatMap(staffProfileRepo::findById)
                .ifPresent(profile -> request.setStaffName(profile.getFullName()));
        Optional.ofNullable(request.getShiftId())
                .flatMap(workShiftRepo::findById)
                .map(WorkShift::getShiftName)
                .ifPresent(request::setShiftName);
        return dtoMapper.toResponse(request);
    }
}
