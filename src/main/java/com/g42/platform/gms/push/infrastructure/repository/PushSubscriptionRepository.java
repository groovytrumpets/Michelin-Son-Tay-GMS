package com.g42.platform.gms.push.infrastructure.repository;

import com.g42.platform.gms.push.infrastructure.entity.PushSubscriptionJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionJpa, Long> {

    Optional<PushSubscriptionJpa> findByEndpoint(String endpoint);

    List<PushSubscriptionJpa> findByStaffIdAndActiveTrue(Integer staffId);

    List<PushSubscriptionJpa> findByActiveTrue();

    List<PushSubscriptionJpa> findByStaffIdOrderByLastUsedAtDesc(Integer staffId);

    @Modifying
    @Transactional
    @Query("UPDATE PushSubscriptionJpa p SET p.active = false WHERE p.endpoint = :endpoint")
    void deactivateByEndpoint(@Param("endpoint") String endpoint);
}
