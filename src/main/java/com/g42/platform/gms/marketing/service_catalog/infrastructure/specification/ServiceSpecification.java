package com.g42.platform.gms.marketing.service_catalog.infrastructure.specification;

import com.g42.platform.gms.marketing.service_catalog.domain.enums.ServiceStatus;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ServiceSpecification {
    public static Specification<ServiceJpaEntity> filterServices(CatalogItemType itemType, BigDecimal maxPrice, BigDecimal minPrice, String categoryCode, Integer brandId, Integer productLineId,String search) {
        return ((root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<CatalogItemJpa> catalogRoot = subquery.from(CatalogItemJpa.class);
            subquery.select(catalogRoot.get("serviceId"));
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(catalogRoot.get("isActive")));

            if (itemType != null) {
                predicates.add(cb.equal(catalogRoot.get("itemType"), itemType));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(catalogRoot.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(catalogRoot.get("price"), maxPrice));
            }
            if (brandId != null) {
                predicates.add(cb.equal(catalogRoot.get("brandId"), brandId));
            }
            if (productLineId != null) {
                // Tùy theo tên biến trong Entity của bạn là productLine hay productLineId
                predicates.add(cb.equal(catalogRoot.get("productLine"), productLineId));
            }

            // Xử lý categoryCode (Nếu bảng CatalogItem chỉ lưu ID mà bạn truyền Code thì sẽ cần Join ở đây)
            // Giả sử bảng Catalog có sẵn field categoryCode:
            if (categoryCode != null && !categoryCode.isEmpty()) {
                predicates.add(cb.equal(catalogRoot.get("categoryCode"), categoryCode));
            }

            // Gắn danh sách điều kiện vào Subquery
            subquery.where(predicates.toArray(new Predicate[0]));

            // 4. Móc Subquery vào Query chính (Service):
            // Lọc ra các Service có ID NẰM TRONG danh sách ID mà Subquery vừa tìm được
            Predicate serviceInCatalog = cb.in(root.get("serviceId")).value(subquery);

            // 5. Thêm điều kiện riêng của Service (Ví dụ: Chỉ lấy bài viết đã PUBLISHED cho khách xem)
            Predicate isPublished = cb.equal(root.get("status"), ServiceStatus.ACTIVE);

            Predicate finalPredicate = cb.and(serviceInCatalog, isPublished);
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";

                Predicate searchInTitle = cb.like(cb.lower(root.get("title")), searchPattern);
                finalPredicate = cb.and(finalPredicate, searchInTitle);
            }
            return finalPredicate;
        });
    }
}
