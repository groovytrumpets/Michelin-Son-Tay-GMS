package com.g42.platform.gms.marketing.service_catalog.application.service;

import com.g42.platform.gms.catalog.service.CatalogItemService;
import com.g42.platform.gms.common.service.ImageUploadService;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceCreateRequest;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceDetailRespond;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceSumaryRespond;
import com.g42.platform.gms.marketing.service_catalog.api.mapper.ServiceDtoMapper;
import com.g42.platform.gms.marketing.service_catalog.domain.entity.ServiceMedia;
import com.g42.platform.gms.marketing.service_catalog.domain.enums.MediaType;
import com.g42.platform.gms.marketing.service_catalog.domain.exception.ServiceErrorCode;
import com.g42.platform.gms.marketing.service_catalog.domain.exception.ServiceException;
import com.g42.platform.gms.marketing.service_catalog.domain.repository.ServiceRepository;
import com.g42.platform.gms.marketing.itempost.domain.ItemPostStatus;
import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostJpa;
import com.g42.platform.gms.marketing.itempost.infrastructure.repository.ItemPostJpaRepo;
import com.g42.platform.gms.warehouse.api.dto.HomeCatalogItemInfoDto;
import com.g42.platform.gms.warehouse.api.internal.WarehouseInternalApi;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;
    private final ServiceDtoMapper serviceDtoMapper;
    private final ImageUploadService imageUploadService;
    private final WarehouseInternalApi warehouseInternalApi;
    private final CatalogItemService catalogItemService;
    private final ItemPostJpaRepo itemPostJpaRepo;

    public List<ServiceSumaryRespond> getListActiveServices() {
        LocalDateTime now = LocalDateTime.now();
        List<ServiceSumaryRespond> respondList = serviceRepository.findAllActive().stream()
                .filter(service -> service.isVisibleNow(now))
                .map(serviceDtoMapper::toDto)
                .toList();
        enrichWithSlugs(respondList);
        return respondList;
    }

    @Transactional(noRollbackFor = ServiceException.class)
    public ServiceDetailRespond getServiceDetailById(Long serviceId) {
        com.g42.platform.gms.marketing.service_catalog.domain.entity.Service service =serviceRepository.findServiceDetailById(serviceId);
        if (service == null) {
            throw new ServiceException("Service not found", ServiceErrorCode.SERVICE_NOT_FOUND);
        }
        if (!service.isVisibleNow(LocalDateTime.now())) {
            throw new ServiceException("Service expired", ServiceErrorCode.SERVICE_EXPIRED);
        }
        return serviceDtoMapper.toDetailDto(service);
    }

    public Long[] getArrayOfCatalogId(Long[] serviceId) {
        return serviceRepository.getCatalogIdByServiceId(serviceId);
    }
    @Transactional
    public ServiceDetailRespond createNewService(ServiceCreateRequest request, Integer catalogId) throws IOException {
        com.g42.platform.gms.marketing.service_catalog.domain.entity.Service
                service = serviceDtoMapper.toEntity(request);
        if (request.getThumbnailFile() != null && !request.getThumbnailFile().isEmpty()) {
            String thumnailUrl = imageUploadService.uploadImage(request.getThumbnailFile(),"garage/services/thumbnails");
            service.setMediaThumbnail(thumnailUrl);
        }
        if (request.getMediaFiles() != null && !request.getMediaFiles().isEmpty()) {
            List<ServiceMedia> mediaList = new ArrayList<>();
            int displayOrder=1;
            for (MultipartFile file : request.getMediaFiles()) {
                if (!file.isEmpty()) {


                    String contentType = file.getContentType();
                        ServiceMedia mediaEntity = new ServiceMedia();
                        mediaEntity.setDisplayOrder(displayOrder++);
                    if (contentType != null && contentType.startsWith("video")) {
                        String mediaUrl = imageUploadService.uploadVideo(file, "garage/services/video");
                        mediaEntity.setMediaUrl(mediaUrl);
                        mediaEntity.setMediaType(MediaType.VIDEO);
                    } else {String mediaUrl = imageUploadService.uploadImage(file, "garage/services/gallery");

                        mediaEntity.setMediaUrl(mediaUrl);
                        mediaEntity.setMediaType(MediaType.IMAGE);
                    }
                    mediaList.add(mediaEntity);
                }
            }
            service.setMedia(mediaList);
        }
        //todo: save catalog
        com.g42.platform.gms.marketing.service_catalog.domain.entity.Service
                serviceSaved = serviceRepository.save(service);
        warehouseInternalApi.updateCatalogBlogService(serviceSaved,catalogId);

        return serviceDtoMapper.toDetailDto(serviceSaved);
    }

    @Transactional
    public ServiceDetailRespond updateService(ServiceCreateRequest request, Long serviceId) throws IOException {
        com.g42.platform.gms.marketing.service_catalog.domain.entity.Service service = serviceRepository.findServiceDetailById(serviceId);
        if (service == null) {
            throw new ServiceException("Service not found", ServiceErrorCode.SERVICE_NOT_FOUND);
        }

        service.setTitle(request.getTitle());
        service.setShortDescription(request.getShortDescription());
        service.setFullDescription(request.getFullDescription());
        service.setShowPrice(request.isShowPrice());
        service.setDisplayPrice(request.getDisplayPrice());
        service.setStatus(request.getStatus());
//        service.setEstimateTime(request.getEstimateTime());

        // Cập nhật ảnh đại diện (Thumbnail)
        if (request.getThumbnailFile() != null && !request.getThumbnailFile().isEmpty()) {
            String thumnailUrl = imageUploadService.uploadImage(request.getThumbnailFile(), "garage/services/thumbnails");
            service.setMediaThumbnail(thumnailUrl);
        } else {
            // Giữ lại URL cũ hoặc xóa (nếu gửi lên rỗng/null)
            String thumbUrl = request.getThumbnailUrl();
            service.setMediaThumbnail(thumbUrl != null && !thumbUrl.trim().isEmpty() ? thumbUrl.trim() : null);
        }

        // Cập nhật thư viện ảnh/video (Media)
        List<ServiceMedia> currentMedia = service.getMedia();
        if (currentMedia == null) {
            currentMedia = new ArrayList<>();
            service.setMedia(currentMedia);
        }

        // Lọc bỏ những media cũ không nằm trong danh sách request.existingMediaUrls
        List<String> existingUrls = request.getExistingMediaUrls() != null ? request.getExistingMediaUrls() : new ArrayList<>();
        List<ServiceMedia> toRemove = new ArrayList<>();
        for (ServiceMedia m : currentMedia) {
            if (m.getMediaUrl() == null || !existingUrls.contains(m.getMediaUrl().trim())) {
                toRemove.add(m);
            }
        }
        currentMedia.removeAll(toRemove);

        // Cập nhật displayOrder cho những media giữ lại dựa theo thứ tự mới gửi lên
        for (int i = 0; i < existingUrls.size(); i++) {
            final String url = existingUrls.get(i).trim();
            final int displayOrderVal = i + 1;
            currentMedia.stream()
                    .filter(m -> url.equals(m.getMediaUrl()))
                    .findFirst()
                    .ifPresent(m -> m.setDisplayOrder(displayOrderVal));
        }

        // Upload thêm các ảnh/video mới
        if (request.getMediaFiles() != null && !request.getMediaFiles().isEmpty()) {
            for (MultipartFile file : request.getMediaFiles()) {
                if (!file.isEmpty()) {
                    String contentType = file.getContentType();
                    ServiceMedia mediaEntity = new ServiceMedia();
                    mediaEntity.setDisplayOrder(currentMedia.size() + 1);
                    if (contentType != null && contentType.startsWith("video")) {
                        String mediaUrl = imageUploadService.uploadVideo(file, "garage/services/video");
                        mediaEntity.setMediaUrl(mediaUrl);
                        mediaEntity.setMediaType(MediaType.VIDEO);
                    } else {
                        String mediaUrl = imageUploadService.uploadImage(file, "garage/services/gallery");
                        mediaEntity.setMediaUrl(mediaUrl);
                        mediaEntity.setMediaType(MediaType.IMAGE);
                    }
                    currentMedia.add(mediaEntity);
                }
            }
        }

        return serviceDtoMapper.toDetailDto(serviceRepository.save(service));
    }

    public Page<ServiceSumaryRespond> getListProducts(int page, int size, CatalogItemType itemType, String search, String sortBy, BigDecimal minPrice, BigDecimal maxPrice, String categoryCode, Integer brandId, Integer productLineId, String vehicleBrand, String vehicleModel) {
        Integer resolvedCategoryId = null;
        if (categoryCode != null) {
            resolvedCategoryId = warehouseInternalApi.findCodeByCategoryCode(categoryCode);
        }
        Page<com.g42.platform.gms.marketing.service_catalog.domain.entity.Service> services =
                serviceRepository.getListOfProductsByCatalogItem(page,size,itemType,search,sortBy,maxPrice,minPrice,resolvedCategoryId,brandId,productLineId,vehicleBrand,vehicleModel);
        Page<ServiceSumaryRespond> dtoPage = services.map(serviceDtoMapper::toDto);
        enrichWithWarehouseInfo(dtoPage.getContent());
        enrichWithSlugs(dtoPage.getContent());
        return dtoPage;
    }

    /** Kho/cửa hàng còn hàng của một item — public cho trang chi tiết phụ tùng. */
    public List<com.g42.platform.gms.warehouse.api.dto.HomeStockLocationDto> getPublicStockLocations(Integer catalogItemId) {
        return warehouseInternalApi.getHomeStockLocations(catalogItemId);
    }

    /**
     * Bổ sung thông tin từ kho (hạng mục báo giá, hãng/dòng, xe tương thích, tồn kho khả dụng)
     * cho danh sách sản phẩm public. Batch một lượt theo catalogItemId để tránh N+1.
     */
    private void enrichWithWarehouseInfo(List<ServiceSumaryRespond> dtos) {
        java.util.Set<Integer> itemIds = new java.util.HashSet<>();
        for (ServiceSumaryRespond dto : dtos) {
            if (dto.getCatalogItemId() > 0) itemIds.add(dto.getCatalogItemId());
        }
        if (itemIds.isEmpty()) return;

        java.util.Map<Integer, HomeCatalogItemInfoDto> infoMap = warehouseInternalApi.getHomeCatalogInfoByItemIds(itemIds);
        for (ServiceSumaryRespond dto : dtos) {
            HomeCatalogItemInfoDto info = infoMap.get(dto.getCatalogItemId());
            if (info == null) continue;
            dto.setItemType(info.getItemType());
            dto.setPrice(info.getPrice());
            dto.setItemCategoryId(info.getItemCategoryId());
            dto.setCategoryCode(info.getCategoryCode());
            dto.setCategoryName(info.getCategoryName());
            dto.setBrandId(info.getBrandId());
            dto.setBrandName(info.getBrandName());
            dto.setProductLineId(info.getProductLineId());
            dto.setProductLineName(info.getProductLineName());
            dto.setCompatibleCars(info.getCompatibleCars());
            Integer availableQty = info.getAvailableQty();
            dto.setAvailableQty(availableQty);
            // inStock chỉ có ý nghĩa với phụ tùng; dịch vụ không quản lý tồn kho
            if ("PART".equalsIgnoreCase(info.getItemType())) {
                dto.setInStock(availableQty != null && availableQty > 0);
            }
        }
    }

    /**
     * Xóa hẳn service (giá/media/thời gian ước tính) gắn với một catalog item — gỡ
     * liên kết catalog_item.service_service_id trước để tránh vi phạm khoá ngoại,
     * rồi xóa service (Hibernate cascade xóa luôn service_media theo orphanRemoval).
     * catalog_item và item_post (bài viết) của mặt hàng không bị ảnh hưởng — sau khi
     * xóa, nút "Sửa bài viết" ở danh sách quay lại thành "Tạo bài viết" để tạo mới.
     */
    @Transactional
    public void deleteService(Long serviceId) {
        com.g42.platform.gms.marketing.service_catalog.domain.entity.Service service =
                serviceRepository.findServiceDetailById(serviceId);
        if (service == null) {
            throw new ServiceException("Service not found", ServiceErrorCode.SERVICE_NOT_FOUND);
        }
        warehouseInternalApi.clearCatalogService(serviceId);
        serviceRepository.deleteById(serviceId);
    }

    /**
     * Gắn slug bài viết item_post PUBLISHED cho từng dòng sản phẩm công khai, tra hàng
     * loạt theo catalogItemId để tránh N+1. Item chưa có bài viết giữ slug null — FE tự
     * fallback về link id số.
     */
    private void enrichWithSlugs(List<ServiceSumaryRespond> dtos) {
        java.util.List<Integer> itemIds = dtos.stream()
                .map(ServiceSumaryRespond::getCatalogItemId)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (itemIds.isEmpty()) return;

        java.util.Map<Integer, String> slugByCatalogItemId = itemPostJpaRepo
                .findByCatalogItemIdInAndStatusAndDeletedAtIsNull(itemIds, ItemPostStatus.PUBLISHED)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        ItemPostJpa::getCatalogItemId,
                        ItemPostJpa::getSlug,
                        (existing, duplicate) -> existing));

        for (ServiceSumaryRespond dto : dtos) {
            dto.setSlug(slugByCatalogItemId.get(dto.getCatalogItemId()));
        }
    }
}
