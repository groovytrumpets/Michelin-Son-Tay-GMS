package com.g42.platform.gms.dashboard.application.service;

import com.g42.platform.gms.dashboard.api.dto.NotificationCreateDto;
import com.g42.platform.gms.dashboard.api.dto.NotificationRespondDto;
import com.g42.platform.gms.dashboard.api.mapper.StaffNotifyDtoMapper;
import com.g42.platform.gms.dashboard.domain.entity.StaffNotification;
import com.g42.platform.gms.dashboard.domain.enums.NotificationType;
import com.g42.platform.gms.dashboard.domain.repository.StaffNotifyRepo;
import com.g42.platform.gms.push.application.service.WebPushDispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StaffNotifyService {
    @Autowired
    private StaffNotifyRepo staffNotifyRepo;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    private StaffNotifyDtoMapper staffNotifyDtoMapper;
    @Autowired
    private WebPushDispatchService webPushDispatchService;

    public void sendNotification(StaffNotification saved) {
        if (saved.getStaffId()==null) {

            simpMessagingTemplate.convertAndSend("/topic/public-notifications", saved);
        }else {
            simpMessagingTemplate.convertAndSendToUser(saved.getStaffId().toString(), "/queue/private-notifications", saved);
        }

        // Đẩy thông báo cấp hệ điều hành (Web Push) để nhận cả khi đã tắt web.
        // Chạy bất đồng bộ; nếu chưa cấu hình VAPID thì tự no-op.
        webPushDispatchService.dispatch(saved);
    }
    @Transactional
    public void createAndSendManual(NotificationCreateDto dto) {
        StaffNotification staffNotification = staffNotifyDtoMapper.toDomain(dto);
        staffNotification.setIsRead(false);
        staffNotification.setSentAt(LocalDateTime.now());

        StaffNotification saved = staffNotifyRepo.save(staffNotification);
        sendNotification(saved);
    }
    @Transactional
    public void createNotificationAssignAuto(Integer staffId, String title, String message, Integer sendBy,String url) {
        NotificationCreateDto dto = new NotificationCreateDto(staffId,title,message, NotificationType.INFO,false,sendBy,url);
        createAndSendManual(dto);
    }

    public List<NotificationRespondDto> getMyNotifications(Integer staffId) {
        List<StaffNotification> staffNotification = staffNotifyRepo.getMyNotification(staffId);
        return staffNotification.stream().map(staffNotifyDtoMapper::toDto).toList();
    }

    public void markAsRead(Integer notificationId) {
        staffNotifyRepo.markAsRead(notificationId);
    }
}
