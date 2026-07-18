package com.g42.platform.gms.feedback.infrastructure;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.dashboard.application.service.StaffNotifyService;
import com.g42.platform.gms.feedback.api.internal.FeedbackInternalApi;
import com.g42.platform.gms.feedback.infrastructure.entity.FeedbackJpa;
import com.g42.platform.gms.feedback.infrastructure.repository.FeedbackJpaRepo;
import com.g42.platform.gms.service_ticket_management.api.internal.ServiceTicketInternalApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class FeedbackIntenalApiImpl implements FeedbackInternalApi {

    // Feedback dưới ngưỡng này (theo thang 5 sao) sẽ được cảnh báo cho manager
    private static final int LOW_RATING_THRESHOLD = 4;
    private static final String MANAGER_FEEDBACK_URL = "/feedback-management";

    @Autowired
    private FeedbackJpaRepo feedbackJpaRepo;
    @Autowired
    private ServiceTicketInternalApi serviceTicketInternalApi;
    @Autowired
    private StaffProfileRepo staffProfileRepo;
    @Autowired
    private StaffNotifyService staffNotifyService;

    @Override
    public void addCusFeedbackRespond(Integer rate, String note, List<String> feedbacks, String trackingId, String submitTime) {
        FeedbackJpa feedbackJpa = new FeedbackJpa();
        feedbackJpa.setStarRating(rate);
        feedbackJpa.setComment(note);
        feedbackJpa.setCreatedAt(Instant.now());
        Integer serviceId = serviceTicketInternalApi.getServiceIdByCode(trackingId);
        feedbackJpa.setServiceTicketId(serviceId);
        String joinedFeedbacks = (feedbacks != null && !feedbacks.isEmpty())
                ? String.join(", ", feedbacks)
                : null;
        feedbackJpa.setDetailFeedback(joinedFeedbacks);
        feedbackJpaRepo.save(feedbackJpa);

        if (rate != null && rate > 0 && rate < LOW_RATING_THRESHOLD) {
            notifyManagersOfLowRating(rate, note, trackingId, serviceId);
        }
    }

    private void notifyManagersOfLowRating(Integer rate, String note, String trackingId, Integer serviceId) {
        try {
            List<StaffProfile> managers = staffProfileRepo.findByRoleCode("MANAGER");
            List<StaffProfile> admins = staffProfileRepo.findByRoleCode("ADMIN");

            // Một nhân viên có thể mang cả 2 role (MANAGER + ADMIN) — gộp và loại trùng theo staffId.
            java.util.Map<Integer, StaffProfile> recipients = new java.util.LinkedHashMap<>();
            java.util.stream.Stream.concat(managers.stream(), admins.stream())
                    .filter(p -> p != null && p.getStaffId() != null)
                    .forEach(p -> recipients.putIfAbsent(p.getStaffId(), p));

            if (recipients.isEmpty()) return;

            String ticketLabel = trackingId != null ? trackingId : (serviceId != null ? "#" + serviceId : "không xác định");
            String title = "Cảnh báo: Feedback đánh giá thấp (" + rate + "/5 sao)";
            StringBuilder message = new StringBuilder("Khách hàng đánh giá ")
                    .append(rate)
                    .append("/5 sao cho phiếu dịch vụ ")
                    .append(ticketLabel)
                    .append(".");
            if (note != null && !note.isBlank()) {
                message.append(" Nhận xét: \"").append(note.trim()).append("\".");
            }
            message.append(" Vui lòng kiểm tra tại trang Quản lý feedback.");

            for (StaffProfile manager : recipients.values()) {
                try {
                    staffNotifyService.createNotificationAssignAuto(
                            manager.getStaffId(),
                            title,
                            message.toString(),
                            manager.getStaffId(),
                            MANAGER_FEEDBACK_URL
                    );
                } catch (Exception ignored) {
                    // Bỏ qua lỗi gửi thông báo cho từng người, không chặn các người còn lại
                }
            }
        } catch (Exception ignored) {
            // Không để lỗi gửi cảnh báo làm hỏng luồng lưu feedback từ webhook Zalo
        }
    }
}
