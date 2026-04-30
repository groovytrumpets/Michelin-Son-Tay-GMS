package com.g42.platform.gms.dashboard.infrastructure;

import com.g42.platform.gms.dashboard.domain.repository.StaffNotifyRepo;
import com.g42.platform.gms.dashboard.infrastructure.repository.StaffNotifyJpaRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class StaffNotifyRepoImpl implements StaffNotifyRepo {
    @Autowired
    private StaffNotifyJpaRepo staffNotifyJpaRepo;
}
