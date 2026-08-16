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
            Integer categoryCode, Integer brandId, Integer productLineId, String search,
            String vehicleBrand, String vehicleModel) {

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
                predicates.add(cb.equal(catalogJoin.get("productLineId"), productLineId));
            }
            if (categoryCode != null) {
                // Đã sửa lại tên trường "itemCategoryId" và dùng biến categoryCode truyền vào
                predicates.add(cb.equal(catalogJoin.get("itemCategoryId"), categoryCode));
            }

            // Lọc theo hãng xe / dòng xe tương thích (LIKE trên cột compatible_cars)
            if (vehicleBrand != null && !vehicleBrand.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(catalogJoin.get("compatibleCars")),
                        "%" + vehicleBrand.trim().toLowerCase() + "%"));
            }
            if (vehicleModel != null && !vehicleModel.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(catalogJoin.get("compatibleCars")),
                        "%" + vehicleModel.trim().toLowerCase() + "%"));
            }

            // Gộp tất cả các điều kiện lại bằng AND
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
