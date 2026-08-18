package com.g42.platform.gms.marketing.service_catalog.infrastructure.repository;

import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceMediaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceMediaJpaRepository extends JpaRepository<ServiceMediaJpaEntity, Integer> {

    /**
     * Xóa hàng loạt (bulk JPQL delete, không nạp entity vào persistence context)
     * — dùng trước khi xóa service để tránh Hibernate duyệt qua quan hệ
     * ServiceJpaEntity.catalogItems (mappedBy phía booking_management.CatalogItemJpa)
     * lúc flush, vốn từng gây TransientObjectException khi xóa qua entity graph.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ServiceMediaJpaEntity m where m.service.serviceId = :serviceId")
    void deleteByServiceId(@Param("serviceId") Long serviceId);
}
