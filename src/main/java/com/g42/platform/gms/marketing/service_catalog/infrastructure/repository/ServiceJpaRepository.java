package com.g42.platform.gms.marketing.service_catalog.infrastructure.repository;

import com.g42.platform.gms.marketing.service_catalog.domain.entity.Service;
import com.g42.platform.gms.marketing.service_catalog.domain.enums.ServiceStatus;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceJpaRepository extends JpaRepository<ServiceJpaEntity, Long>, JpaSpecificationExecutor<ServiceJpaEntity> {

    // Nạp kèm media trong cùng truy vấn (mapper entity->domain đọc collection này cho
    // mọi service -> để LAZY là N+1). catalogItems để hibernate.default_batch_fetch_size
    // gom thành 1 câu IN (...); không fetch chung 1 graph để tránh nhân bản hàng do
    // cartesian giữa 2 collection. Query phân trang (getListOfProductsByCatalogItem)
    // KHÔNG dùng EntityGraph được -> cũng dựa vào default_batch_fetch_size.
    @EntityGraph(attributePaths = {"media"})
    List<ServiceJpaEntity> findAllByStatus(ServiceStatus status);

    @EntityGraph(attributePaths = {"media"})
    ServiceJpaEntity searchByServiceId(Long serviceId);

    /**
     * Xóa hàng loạt (bulk JPQL delete) thay vì findById()+remove() — tránh Hibernate
     * duyệt quan hệ ServiceJpaEntity.catalogItems (mappedBy phía
     * booking_management.CatalogItemJpa.serviceService) lúc flush, từng gây
     * TransientObjectException dù không có cascade nào được khai báo trên quan hệ đó.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ServiceJpaEntity s where s.serviceId = :serviceId")
    void deleteServiceById(@Param("serviceId") Long serviceId);
}
