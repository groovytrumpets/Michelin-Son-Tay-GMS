package com.g42.platform.gms.marketing.service_catalog.infrastructure;

import com.g42.platform.gms.marketing.service_catalog.domain.entity.Service;
import com.g42.platform.gms.marketing.service_catalog.domain.enums.ServiceStatus;
import com.g42.platform.gms.marketing.service_catalog.domain.repository.ServiceRepository;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.mapper.ServiceMapper;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.repository.CatalogRepoJpa;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.repository.ServiceJpaRepository;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.specification.ServiceSpecification;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
@AllArgsConstructor
public class ServiceRepositoryImpl implements ServiceRepository {
    private final ServiceJpaRepository serviceJpaRepository;
    private final CatalogRepoJpa catalogRepo;
    private final ServiceMapper serviceMapper;

    @Override
    public List<Service> findAllActive() {
        List<ServiceJpaEntity> serviceJpaEntities = serviceJpaRepository.findAllByStatus(ServiceStatus.ACTIVE);
        return serviceMapper.toDomain(serviceJpaEntities);
    }

    @Override
    public Service findServiceDetailById(Long serviceId) {
        ServiceJpaEntity serviceDetailJpa = serviceJpaRepository.searchByServiceId(serviceId);
        return serviceMapper.toDomain(serviceDetailJpa);
    }

    @Override
    public Long[] getCatalogIdByServiceId(Long[] serviceId) {
        List<Long> catalogIds =
                catalogRepo.findCatalogIdsByServiceIds(List.of(serviceId));

        return catalogIds.toArray(new Long[0]);
    }

    @Override
    public Service save(Service service) {
        ServiceJpaEntity serviceJpaEntity = serviceJpaRepository.save(serviceMapper.toJpaEntity(service));
        return serviceMapper.toDomain(serviceJpaEntity);
    }

    @Override
    public Page<Service> getListOfProductsByCatalogItem(int page, int size, CatalogItemType itemType, String search, String sortBy, BigDecimal maxPrice, BigDecimal minPrice, String categoryCode, Integer brandId, Integer productLineId) {
        Sort sort = Sort.by(Sort.Direction.ASC, "createdAt");
        if (sortBy != null && !sortBy.isEmpty()) {
            String[] parts = sortBy.split(",");
            String field = parts[0];
            Sort.Direction direction = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1]))
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(direction, field);
        }
        Pageable pageable = PageRequest.of(page, size, sort);
        Specification<ServiceJpaEntity> specification = Specification.unrestricted();
        specification = specification.and(ServiceSpecification.
                filterServices(itemType,maxPrice,minPrice,categoryCode,brandId,productLineId,search));
        return serviceJpaRepository.findAll(specification,pageable).map(serviceMapper::toDomain);
    }
}
