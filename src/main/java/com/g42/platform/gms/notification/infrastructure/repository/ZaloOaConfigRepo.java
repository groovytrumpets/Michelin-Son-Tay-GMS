package com.g42.platform.gms.notification.infrastructure.repository;

import com.g42.platform.gms.notification.infrastructure.entity.ZaloOaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZaloOaConfigRepo extends JpaRepository<ZaloOaConfig, Integer> {
}
