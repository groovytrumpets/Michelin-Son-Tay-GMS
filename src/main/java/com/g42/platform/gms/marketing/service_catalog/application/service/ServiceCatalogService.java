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

    public List<ServiceSumaryRespond> getListActiveServices() {
        LocalDateTime now = LocalDateTime.now();
        return serviceRepository.findAllActive().stream().filter(service -> service.isVisibleNow(now)).map(serviceDtoMapper::toDto).toList();
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

    public Page<ServiceSumaryRespond> getListProducts(int page, int size, CatalogItemType itemType, String search, String sortBy, BigDecimal minPrice, BigDecimal maxPrice, String categoryCode, Integer brandId, Integer productLineId) {
        Integer resolvedCategoryId = null;
        if (categoryCode != null) {
            resolvedCategoryId = warehouseInternalApi.findCodeByCategoryCode(categoryCode);
        }
        Page<com.g42.platform.gms.marketing.service_catalog.domain.entity.Service> services =
                serviceRepository.getListOfProductsByCatalogItem(page,size,itemType,search,sortBy,maxPrice,minPrice,resolvedCategoryId,brandId,productLineId);
        return services.map(serviceDtoMapper::toDto);
    }
}
