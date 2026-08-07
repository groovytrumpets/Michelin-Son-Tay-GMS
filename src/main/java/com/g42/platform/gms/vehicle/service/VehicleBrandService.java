package com.g42.platform.gms.vehicle.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.g42.platform.gms.vehicle.dto.VehicleBrandDto;
import com.g42.platform.gms.vehicle.entity.VehicleBrand;
import com.g42.platform.gms.vehicle.entity.VehicleModel;
import com.g42.platform.gms.vehicle.repository.VehicleBrandRepository;
import com.g42.platform.gms.vehicle.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Danh mục hãng xe / dòng xe. Trước đây danh sách này hard-code trong frontend;
 * giờ lưu ở DB và cho phép nhân viên tự bổ sung, hoặc nạp thêm dòng xe từ API
 * công khai NHTSA vPIC (miễn phí, không cần API key).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleBrandService {

    private static final String NHTSA_MODELS_URL =
            "https://vpic.nhtsa.dot.gov/api/vehicles/GetModelsForMake/%s?format=json";

    private final VehicleBrandRepository vehicleBrandRepository;
    private final VehicleModelRepository vehicleModelRepository;

    private final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        // Không dùng RestTemplateBuilder: nó tự dò Apache HttpClient5 trên
        // classpath, bản trong dự án không tương thích Spring Boot 3.5.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(20));
        return new RestTemplate(factory);
    }

    /**
     * @param activeOnly chỉ lấy hãng đang bật
     * @param withModels kèm danh sách dòng xe của từng hãng
     */
    @Transactional(readOnly = true)
    public List<VehicleBrandDto> findAll(boolean activeOnly, boolean withModels) {
        List<VehicleBrand> brands = activeOnly
                ? vehicleBrandRepository.findByActiveTrueOrderBySortOrderAscNameAsc()
                : vehicleBrandRepository.findAllByOrderBySortOrderAscNameAsc();

        Map<Integer, List<String>> modelsByBrand = Map.of();
        if (withModels) {
            List<VehicleModel> models = activeOnly
                    ? vehicleModelRepository.findByActiveTrueOrderByNameAsc()
                    : vehicleModelRepository.findAll();
            modelsByBrand = models.stream().collect(Collectors.groupingBy(
                    VehicleModel::getBrandId,
                    Collectors.mapping(VehicleModel::getName, Collectors.toList())));
        }

        List<VehicleBrandDto> result = new ArrayList<>(brands.size());
        for (VehicleBrand brand : brands) {
            result.add(new VehicleBrandDto(
                    brand.getBrandId(),
                    brand.getName(),
                    brand.getCountry(),
                    brand.getLogoUrl(),
                    brand.getActive(),
                    withModels ? modelsByBrand.getOrDefault(brand.getBrandId(), List.of()) : null
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<String> findModels(Integer brandId, boolean activeOnly) {
        List<VehicleModel> models = activeOnly
                ? vehicleModelRepository.findByBrandIdAndActiveTrueOrderByNameAsc(brandId)
                : vehicleModelRepository.findByBrandIdOrderByNameAsc(brandId);
        return models.stream().map(VehicleModel::getName).toList();
    }

    /** Thêm hãng xe mới; trùng tên thì trả về hãng sẵn có (và bật lại nếu đang tắt). */
    @Transactional
    public VehicleBrandDto createBrand(VehicleBrandDto dto) {
        String name = normalizeBrandName(dto.getName());
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Tên hãng xe không được để trống");
        }

        VehicleBrand brand = vehicleBrandRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            VehicleBrand created = new VehicleBrand();
            created.setName(name);
            created.setCreatedAt(LocalDateTime.now());
            return created;
        });
        brand.setActive(true);
        if (dto.getCountry() != null) brand.setCountry(dto.getCountry());
        if (dto.getLogoUrl() != null) brand.setLogoUrl(dto.getLogoUrl());

        brand = vehicleBrandRepository.save(brand);
        return new VehicleBrandDto(brand.getBrandId(), brand.getName(), brand.getCountry(),
                brand.getLogoUrl(), brand.getActive(), List.of());
    }

    @Transactional
    public VehicleBrandDto updateBrand(Integer brandId, VehicleBrandDto dto) {
        VehicleBrand brand = vehicleBrandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hãng xe"));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            brand.setName(normalizeBrandName(dto.getName()));
        }
        if (dto.getCountry() != null) brand.setCountry(dto.getCountry());
        if (dto.getLogoUrl() != null) brand.setLogoUrl(dto.getLogoUrl());
        if (dto.getActive() != null) brand.setActive(dto.getActive());

        brand = vehicleBrandRepository.save(brand);
        return new VehicleBrandDto(brand.getBrandId(), brand.getName(), brand.getCountry(),
                brand.getLogoUrl(), brand.getActive(), null);
    }

    /** Ngừng sử dụng thay vì xoá cứng để không ảnh hưởng xe đã gắn hãng này. */
    @Transactional
    public void deactivateBrand(Integer brandId) {
        VehicleBrand brand = vehicleBrandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hãng xe"));
        brand.setActive(false);
        vehicleBrandRepository.save(brand);
    }

    @Transactional
    public String addModel(Integer brandId, String rawName) {
        vehicleBrandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hãng xe"));

        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Tên dòng xe không được để trống");
        }

        VehicleModel model = vehicleModelRepository.findByBrandIdAndNameIgnoreCase(brandId, name)
                .orElseGet(() -> {
                    VehicleModel created = new VehicleModel();
                    created.setBrandId(brandId);
                    created.setName(name);
                    created.setCreatedAt(LocalDateTime.now());
                    return created;
                });
        model.setActive(true);
        return vehicleModelRepository.save(model).getName();
    }

    /**
     * Nạp danh sách dòng xe của một hãng từ NHTSA vPIC và thêm những dòng chưa có.
     *
     * @return số dòng xe được thêm mới
     */
    @Transactional
    public int importModelsFromNhtsa(Integer brandId) {
        VehicleBrand brand = vehicleBrandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hãng xe"));

        JsonNode body;
        try {
            String url = String.format(NHTSA_MODELS_URL,
                    brand.getName().trim().replace(" ", "%20"));
            body = restTemplate.getForObject(url, JsonNode.class);
        } catch (RestClientException e) {
            log.warn("NHTSA lookup failed for brand {}: {}", brand.getName(), e.getMessage());
            throw new IllegalStateException("Không kết nối được dịch vụ tra cứu dòng xe. Vui lòng thử lại sau.");
        }

        JsonNode results = body == null ? null : body.path("Results");
        if (results == null || !results.isArray() || results.isEmpty()) {
            return 0;
        }

        Set<String> existing = vehicleModelRepository.findByBrandIdOrderByNameAsc(brandId).stream()
                .map(model -> model.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        // LinkedHashSet để bỏ trùng trong chính response mà vẫn giữ thứ tự.
        Set<String> incoming = new LinkedHashSet<>();
        for (JsonNode node : results) {
            String name = node.path("Model_Name").asText("").trim();
            if (!name.isEmpty() && !existing.contains(name.toLowerCase(Locale.ROOT))) {
                incoming.add(name);
            }
        }

        List<VehicleModel> toSave = new ArrayList<>(incoming.size());
        for (String name : incoming) {
            VehicleModel model = new VehicleModel();
            model.setBrandId(brandId);
            model.setName(name);
            model.setCreatedAt(LocalDateTime.now());
            toSave.add(model);
        }
        vehicleModelRepository.saveAll(toSave);
        return toSave.size();
    }

    private String normalizeBrandName(String name) {
        return name == null ? "" : name.trim().toUpperCase(Locale.ROOT);
    }
}
