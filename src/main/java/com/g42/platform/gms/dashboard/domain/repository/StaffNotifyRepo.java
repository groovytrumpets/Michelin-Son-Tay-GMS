package com.g42.platform.gms.dashboard.domain.repository;

import com.g42.platform.gms.dashboard.domain.entity.StaffNotification;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffNotifyRepo {
    StaffNotification save(StaffNotification staffNotification);

    List<StaffNotification> getMyNotification(Integer staffId);

    void markAsRead(Integer notificationId);
}
