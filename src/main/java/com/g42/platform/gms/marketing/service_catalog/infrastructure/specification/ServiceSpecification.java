package com.g42.platform.gms.marketing.service_catalog.infrastructure.specification;

import com.g42.platform.gms.marketing.service_catalog.domain.enums.ServiceStatus;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ServiceSpecification {
    public static Specification<ServiceJpaEntity> filterServices(
            CatalogItemType itemType, BigDecimal maxPrice, BigDecimal minPrice,
            Integer categoryCode, Integer brandId, Integer productLineId, String search) {

        return (root, query, cb) -> {
            // QUAN TRỌNG: Loại bỏ các Service bị trùng lặp trong kết quả trả về
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            // 1. Điều kiện của bảng Cha (ServiceJpaEntity)
            predicates.add(cb.equal(root.get("status"), ServiceStatus.ACTIVE));

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("title")), searchPattern));
            }

            // 2. JOIN sang bảng Con (CatalogItemJpa) để lọc các thuộc tính của Item
            // "catalogItems" phải đúng với tên biến Set<CatalogItemJpa> trong ServiceJpaEntity
            Join<ServiceJpaEntity, CatalogItemJpa> catalogJoin = root.join("catalogItems");

            predicates.add(cb.isTrue(catalogJoin.get("isActive")));

            // 3. Các điều kiện của bảng Con
            if (itemType != null) {
                predicates.add(cb.equal(catalogJoin.get("itemType"), itemType.name()));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(catalogJoin.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(catalogJoin.get("price"), maxPrice));
            }
            if (brandId != null) {
                predicates.add(cb.equal(catalogJoin.get("brandId"), brandId));
            }
            if (productLineId != null) {
                // Đảm bảo tên "productLine" khớp với tên biến trong CatalogItemJpa
                predicates.add(cb.equal(catalogJoin.get("productLine"), productLineId));
            }
            if (categoryCode != null) {
                // Đã sửa lại tên trường "workCategoryId" và dùng biến categoryCode truyền vào
                predicates.add(cb.equal(catalogJoin.get("workCategoryId"), categoryCode));
            }

            // Gộp tất cả các điều kiện lại bằng AND
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
