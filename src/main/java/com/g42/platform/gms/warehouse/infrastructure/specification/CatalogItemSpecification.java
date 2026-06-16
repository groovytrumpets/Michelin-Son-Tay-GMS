package com.g42.platform.gms.warehouse.infrastructure.specification;

import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CatalogItemSpecification {
    public static Specification<CatalogItemJpa> filterCatalog(CatalogItemType itemType, Boolean isActive, Integer brandId, Integer productLineId, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String vehicleBrand, String vehicleModel) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (itemType != null) predicates.add(cb.equal(root.get("itemType"), itemType));
            if (isActive != null) predicates.add(cb.equal(root.get("isActive"), isActive));
            if (brandId != null) predicates.add(cb.equal(root.get("brandId"), brandId));
            if (productLineId != null) predicates.add(cb.equal(root.get("productLineId"), productLineId));
            if (categoryId != null) predicates.add(cb.equal(root.get("workCategoryId"), categoryId));
            if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));

            if ((vehicleBrand != null && !vehicleBrand.trim().isEmpty()) || (vehicleModel != null && !vehicleModel.trim().isEmpty())) {
                List<Predicate> carPredicates = new ArrayList<>();
                carPredicates.add(cb.isNull(root.get("compatibleCars")));
                carPredicates.add(cb.equal(root.get("compatibleCars"), ""));
                if (vehicleBrand != null && !vehicleBrand.trim().isEmpty()) {
                    carPredicates.add(cb.like(cb.lower(root.get("compatibleCars")), "%" + vehicleBrand.toLowerCase().trim() + "%"));
                }
                if (vehicleModel != null && !vehicleModel.trim().isEmpty()) {
                    String modelLower = vehicleModel.toLowerCase().trim();
                    carPredicates.add(cb.like(cb.lower(root.get("compatibleCars")), "%" + modelLower + "%"));
                    String[] modelWords = modelLower.split("\\s+");
                    if (modelWords.length > 0) {
                        carPredicates.add(cb.like(cb.lower(root.get("compatibleCars")), "%" + modelWords[0] + "%"));
                    }
                }
                predicates.add(cb.or(carPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
