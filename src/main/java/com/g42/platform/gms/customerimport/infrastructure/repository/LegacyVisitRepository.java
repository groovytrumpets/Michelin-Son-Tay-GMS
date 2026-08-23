package com.g42.platform.gms.customerimport.infrastructure.repository;

import com.g42.platform.gms.customerimport.infrastructure.entity.LegacyVisitJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LegacyVisitRepository extends JpaRepository<LegacyVisitJpa, Integer> {

    boolean existsByDedupeKey(String dedupeKey);

    /** Dùng ở bước kiểm tra thử: hỏi một lần cho cả file thay vì mỗi dòng một truy vấn. */
    @Query("select v.dedupeKey from LegacyVisitJpa v where v.dedupeKey in :keys")
    List<String> findExistingDedupeKeys(@Param("keys") Collection<String> keys);

    List<LegacyVisitJpa> findByImportBatchId(Integer importBatchId);

    List<LegacyVisitJpa> findByCustomerIdOrderByVisitedAtDesc(Integer customerId);

    long countByCustomerId(Integer customerId);

    long countByVehicleId(Integer vehicleId);

    void deleteByImportBatchId(Integer importBatchId);

    /** Lần cuối và số lần đến xưởng theo sổ cũ, gom nhóm sẵn để màn danh sách không bị N+1. */
    @Query("""
        select new com.g42.platform.gms.customerimport.api.dto.VisitAggregateDto(
            lv.customerId, max(lv.visitedAt), count(lv)
        )
        from LegacyVisitJpa lv
        where lv.customerId in :customerIds
        group by lv.customerId
    """)
    List<com.g42.platform.gms.customerimport.api.dto.VisitAggregateDto> aggregateLegacyVisits(
            @Param("customerIds") Collection<Integer> customerIds);

    /**
     * Cùng phép gom nhóm nhưng trên phiếu dịch vụ thật. Đặt ở đây để hai nguồn dùng
     * chung một định nghĩa "một lần đến xưởng" — lệch định nghĩa thì con số cộng lại
     * không còn nghĩa gì.
     */
    @Query("""
        select new com.g42.platform.gms.customerimport.api.dto.VisitAggregateDto(
            st.customerId, max(st.receivedAt), count(st)
        )
        from ServiceTicketManagement st
        where st.customerId in :customerIds
          and (st.isDeleted = false or st.isDeleted is null)
          and st.ticketStatus in ('COMPLETED', 'PAID', 'CANCELLED')
          and st.receivedAt is not null
        group by st.customerId
    """)
    List<com.g42.platform.gms.customerimport.api.dto.VisitAggregateDto> aggregateTicketVisits(
            @Param("customerIds") Collection<Integer> customerIds);
}
