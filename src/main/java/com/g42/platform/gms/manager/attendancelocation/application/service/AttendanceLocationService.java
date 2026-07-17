package com.g42.platform.gms.manager.attendancelocation.application.service;

import com.g42.platform.gms.manager.attendance.domain.exception.AttendanceErrorCode;
import com.g42.platform.gms.manager.attendance.domain.exception.AttendanceException;
import com.g42.platform.gms.manager.attendancelocation.api.dto.AttendanceLocationRequest;
import com.g42.platform.gms.manager.attendancelocation.api.dto.AttendanceLocationResponse;
import com.g42.platform.gms.manager.attendancelocation.api.mapper.AttendanceLocationDtoMapper;
import com.g42.platform.gms.manager.attendancelocation.domain.entity.AttendanceLocation;
import com.g42.platform.gms.manager.attendancelocation.domain.repository.AttendanceLocationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceLocationService {

    private static final int MAX_TOKEN_RETRIES = 5;

    private final AttendanceLocationRepo locationRepo;
    private final AttendanceLocationDtoMapper dtoMapper;

    public List<AttendanceLocationResponse> getAllLocations() {
        return locationRepo.findAll().stream().map(dtoMapper::toResponse).toList();
    }

    @Transactional
    public AttendanceLocationResponse createLocation(AttendanceLocationRequest request) {
        AttendanceLocation location = new AttendanceLocation();
        location.setLocationName(request.getLocationName());
        location.setAddress(request.getAddress());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setRadiusMeters(request.getRadiusMeters() != null ? request.getRadiusMeters() : 100);
        location.setQrToken(generateUniqueQrToken());
        location.setIsActive(true);
        location.setCreatedAt(LocalDateTime.now());
        location.setUpdatedAt(LocalDateTime.now());
        return dtoMapper.toResponse(locationRepo.save(location));
    }

    @Transactional
    public AttendanceLocationResponse updateLocation(Integer locationId, AttendanceLocationRequest request) {
        AttendanceLocation location = findLocation(locationId);
        location.setLocationName(request.getLocationName());
        location.setAddress(request.getAddress());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        if (request.getRadiusMeters() != null) {
            location.setRadiusMeters(request.getRadiusMeters());
        }
        location.setUpdatedAt(LocalDateTime.now());
        return dtoMapper.toResponse(locationRepo.save(location));
    }

    @Transactional
    public void deactivateLocation(Integer locationId) {
        AttendanceLocation location = findLocation(locationId);
        location.setIsActive(false);
        location.setUpdatedAt(LocalDateTime.now());
        locationRepo.save(location);
    }

    @Transactional
    public void reactivateLocation(Integer locationId) {
        AttendanceLocation location = findLocation(locationId);
        location.setIsActive(true);
        location.setUpdatedAt(LocalDateTime.now());
        locationRepo.save(location);
    }

    @Transactional
    public AttendanceLocationResponse regenerateQr(Integer locationId) {
        AttendanceLocation location = findLocation(locationId);
        location.setQrToken(generateUniqueQrToken());
        location.setUpdatedAt(LocalDateTime.now());
        return dtoMapper.toResponse(locationRepo.save(location));
    }

    private AttendanceLocation findLocation(Integer locationId) {
        return locationRepo.findById(locationId)
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.LOCATION_NOT_FOUND));
    }

    private String generateUniqueQrToken() {
        String token;
        int retries = 0;
        do {
            token = UUID.randomUUID().toString().replace("-", "");
            retries++;
        } while (locationRepo.existsByQrToken(token) && retries < MAX_TOKEN_RETRIES);
        return token;
    }
}
