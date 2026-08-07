package com.g42.platform.gms.customer.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.g42.platform.gms.customer.api.dto.LocationDto;
import com.g42.platform.gms.customer.domain.exception.CustomerErrorCode;
import com.g42.platform.gms.customer.domain.exception.CustomerException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nạp danh mục đơn vị hành chính Việt Nam từ provinces.open-api.vn.
 * Kết quả được cache trong bộ nhớ vì dữ liệu gần như không đổi.
 */
@Service
public class LocationService {

    private static final String BASE_URL = "https://provinces.open-api.vn/api/v1";

    private final RestTemplate restTemplate;
    private final Map<String, List<LocationDto>> cache = new ConcurrentHashMap<>();

    public LocationService() {
        // Xem ghi chú ở TaxLookupService: tránh RestTemplateBuilder vì nó tự dò
        // Apache HttpClient5 (bản trên classpath không tương thích Boot 3.5).
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.restTemplate = new RestTemplate(factory);
    }

    /** Danh sách tỉnh/thành. */
    public List<LocationDto> getProvinces() {
        return cache.computeIfAbsent("provinces",
                key -> fetchList(BASE_URL + "/p/", null));
    }

    /** Danh sách quận/huyện của một tỉnh. */
    public List<LocationDto> getDistricts(String provinceId) {
        requireId(provinceId, "Thiếu mã tỉnh/thành!");
        return cache.computeIfAbsent("districts:" + provinceId,
                key -> fetchList(BASE_URL + "/p/" + provinceId + "?depth=2", "districts"));
    }

    /** Danh sách xã/phường của một quận/huyện. */
    public List<LocationDto> getWards(String districtId) {
        requireId(districtId, "Thiếu mã quận/huyện!");
        return cache.computeIfAbsent("wards:" + districtId,
                key -> fetchList(BASE_URL + "/d/" + districtId + "?depth=2", "wards"));
    }

    private void requireId(String id, String message) {
        if (id == null || id.isBlank() || !id.matches("\\d{1,10}")) {
            throw new CustomerException(message, CustomerErrorCode.INVALID_ID);
        }
    }

    /**
     * @param url       endpoint cần gọi
     * @param arrayNode tên field chứa mảng con; null nghĩa là response chính là mảng
     */
    private List<LocationDto> fetchList(String url, String arrayNode) {
        JsonNode body;
        try {
            body = restTemplate.getForObject(url, JsonNode.class);
        } catch (RestClientException e) {
            throw new CustomerException(
                    "Không tải được danh mục địa giới hành chính. Vui lòng nhập tay hoặc thử lại sau.",
                    CustomerErrorCode.INVALID_CONTACT_INFO);
        }

        JsonNode array = body;
        if (arrayNode != null && body != null) {
            array = body.path(arrayNode);
        }
        if (array == null || !array.isArray()) {
            return List.of();
        }

        List<LocationDto> result = new ArrayList<>();
        for (JsonNode node : array) {
            String code = node.path("code").asText(null);
            String name = node.path("name").asText(null);
            if (code != null && name != null) {
                result.add(new LocationDto(code, name));
            }
        }
        return result;
    }
}
