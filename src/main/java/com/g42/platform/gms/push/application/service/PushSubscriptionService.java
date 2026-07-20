package com.g42.platform.gms.push.application.service;

import com.g42.platform.gms.push.api.dto.PushDeviceDto;
import com.g42.platform.gms.push.api.dto.PushSubscriptionRequest;
import com.g42.platform.gms.push.infrastructure.entity.PushSubscriptionJpa;
import com.g42.platform.gms.push.infrastructure.repository.PushSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PushSubscriptionService {

    @Autowired
    private PushSubscriptionRepository repository;

    /**
     * Lưu (upsert theo endpoint) subscription của nhân viên hiện tại. Nếu endpoint đã
     * tồn tại — kể cả trước đó thuộc nhân viên khác trên cùng máy — thì gán lại cho
     * nhân viên đang đăng nhập và kích hoạt lại.
     */
    @Transactional
    public void save(Integer staffId, PushSubscriptionRequest req) {
        if (staffId == null) {
            throw new IllegalStateException("Không xác định được nhân viên đăng nhập.");
        }
        if (req == null || req.getEndpoint() == null || req.getKeys() == null
                || req.getKeys().getP256dh() == null || req.getKeys().getAuth() == null) {
            throw new IllegalArgumentException("Thiếu thông tin subscription (endpoint/keys).");
        }

        PushSubscriptionJpa entity = repository.findByEndpoint(req.getEndpoint())
                .orElseGet(PushSubscriptionJpa::new);

        entity.setStaffId(staffId);
        entity.setEndpoint(req.getEndpoint());
        entity.setP256dh(req.getKeys().getP256dh());
        entity.setAuth(req.getKeys().getAuth());
        entity.setExpirationTime(req.getExpirationTime());
        entity.setUserAgent(req.getUserAgent());
        entity.setDeviceLabel(req.getDeviceLabel());
        entity.setActive(true);

        repository.save(entity);
    }

    /** Huỷ đăng ký thiết bị (set active=false). */
    @Transactional
    public void deactivate(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) return;
        repository.deactivateByEndpoint(endpoint);
    }

    @Transactional(readOnly = true)
    public List<PushDeviceDto> listDevices(Integer staffId) {
        return repository.findByStaffIdOrderByLastUsedAtDesc(staffId).stream()
                .map(p -> new PushDeviceDto(
                        p.getId(),
                        p.getUserAgent(),
                        p.getDeviceLabel(),
                        p.getActive(),
                        p.getCreatedAt(),
                        p.getLastUsedAt()))
                .toList();
    }
}
