package com.g42.platform.gms.dashboard.infrastructure;

import com.g42.platform.gms.dashboard.domain.entity.StaffNotification;
import com.g42.platform.gms.dashboard.domain.repository.StaffNotifyRepo;
import com.g42.platform.gms.dashboard.infrastructure.entity.StaffNotificationJpa;
import com.g42.platform.gms.dashboard.infrastructure.mapper.StaffNotifyJpaMapper;
import com.g42.platform.gms.dashboard.infrastructure.repository.StaffNotifyJpaRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StaffNotifyRepoImpl implements StaffNotifyRepo {
    @Autowired
    private StaffNotifyJpaRepo staffNotifyJpaRepo;
    @Autowired
    private StaffNotifyJpaMapper staffNotifyJpaMapper;

    @Override
    public StaffNotification save(StaffNotification staffNotification) {
        StaffNotificationJpa staffNotificationJpa = staffNotifyJpaMapper.toJpa(staffNotification);
        StaffNotificationJpa saved = staffNotifyJpaRepo.save(staffNotificationJpa);
        return staffNotifyJpaMapper.toDomain(saved);
    }

    @Override
    public List<StaffNotification> getMyNotification(Integer staffId) {
        List<StaffNotificationJpa> staffNotificationJpas = staffNotifyJpaRepo.findAllByStaffIdOrStaffIdIsNull(staffId);
        return staffNotificationJpas.stream().map(staffNotifyJpaMapper::toDomain).toList();
    }

    @Override
    public void markAsRead(Integer notificationId) {
        StaffNotificationJpa staffNotificationJpa =  staffNotifyJpaRepo.findById(notificationId).orElse(null);
        if(staffNotificationJpa==null){
            throw new NullPointerException("Staff Notification Not Found");
        }
        staffNotificationJpa.setIsRead(true);
        staffNotifyJpaRepo.save(staffNotificationJpa);
    }
}
