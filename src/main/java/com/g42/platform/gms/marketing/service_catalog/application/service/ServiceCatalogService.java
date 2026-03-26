package com.g42.platform.gms.marketing.service_catalog.application.service;

import com.g42.platform.gms.auth.exception.AuthException;
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
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;
    private final ServiceDtoMapper serviceDtoMapper;
    private final ImageUploadService imageUploadService;

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
    public ServiceDetailRespond createNewService(ServiceCreateRequest request) throws IOException {
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
                    String mediaUrl = imageUploadService.uploadImage(file, "garage/services/gallery");
                    ServiceMedia mediaEntity = new ServiceMedia();
                    mediaEntity.setMediaUrl(mediaUrl);
                    mediaEntity.setDisplayOrder(displayOrder++);

                    String contentType = file.getContentType();
                    if (contentType != null && contentType.startsWith("video")) {
                        mediaEntity.setMediaType(MediaType.VIDEO);
                    } else {
                        mediaEntity.setMediaType(MediaType.IMAGE);
                    }
                    mediaList.add(mediaEntity);
                }
            }
            service.setMedia(mediaList);
        }

        return serviceDtoMapper.toDetailDto(serviceRepository.save(service));
    }
}
