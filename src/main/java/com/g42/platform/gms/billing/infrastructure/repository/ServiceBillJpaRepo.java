package com.g42.platform.gms.billing.infrastructure.repository;

import com.g42.platform.gms.billing.infrastructure.entity.ServiceBillJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServiceBillJpaRepo extends JpaRepository<ServiceBillJpa,Integer> {
    ServiceBillJpa findByServiceTicketId(Integer serviceTicketId);

    /** Lấy tất cả hoá đơn của một loạt phiếu dịch vụ — dùng cho báo cáo tổng hợp. */
    List<ServiceBillJpa> findByServiceTicketIdIn(Collection<Integer> serviceTicketIds);
    @Query("""
        select b from ServiceBillJpa b where b.serviceTicketId=:serviceTicketId order by b.paidAt desc limit 1
        """)
    ServiceBillJpa findByServiceTicketIdOrderByEstimateIdAsc(Integer serviceTicketId);
}
