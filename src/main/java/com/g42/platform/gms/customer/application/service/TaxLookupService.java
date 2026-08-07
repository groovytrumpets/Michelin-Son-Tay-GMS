package com.g42.platform.gms.customer.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.g42.platform.gms.customer.api.dto.TaxLookupDto;
import com.g42.platform.gms.customer.domain.exception.CustomerErrorCode;
import com.g42.platform.gms.customer.domain.exception.CustomerException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tra cứu thông tin doanh nghiệp theo mã số thuế qua API công khai của VietQR.
 * Dùng để tự động điền tên và địa chỉ khi nhập hồ sơ đối tác.
 */
@Service
public class TaxLookupService {

    private static final String VIETQR_BUSINESS_URL = "https://api.vietqr.io/v2/business/";

    private final RestTemplate restTemplate;
    private final Map<String, TaxLookupDto> cache = new ConcurrentHashMap<>();

    public TaxLookupService() {
        // Dùng thẳng SimpleClientHttpRequestFactory thay vì RestTemplateBuilder:
        // builder tự dò Apache HttpClient5 trên classpath, nhưng bản httpclient5
        // của dự án cũ hơn bản Spring Boot 3.5 yêu cầu nên khởi tạo sẽ lỗi.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.restTemplate = new RestTemplate(factory);
    }

    public TaxLookupDto lookup(String rawTaxCode) {
        String taxCode = rawTaxCode == null ? "" : rawTaxCode.trim().replaceAll("\\s", "");
        // MST Việt Nam: 10 số, hoặc 13 ký tự dạng 10 số - 3 số (đơn vị phụ thuộc).
        if (!taxCode.matches("\\d{10}(-\\d{3})?")) {
            throw new CustomerException("Mã số thuế không hợp lệ (10 số hoặc 10 số-3 số).",
                    CustomerErrorCode.INVALID_ID);
        }

        TaxLookupDto cached = cache.get(taxCode);
        if (cached != null) return cached;

        JsonNode body;
        try {
            body = restTemplate.getForObject(VIETQR_BUSINESS_URL + taxCode, JsonNode.class);
        } catch (RestClientException e) {
            throw new CustomerException("Không kết nối được dịch vụ tra cứu mã số thuế. Vui lòng nhập tay.",
                    CustomerErrorCode.INVALID_CONTACT_INFO);
        }

        JsonNode data = body == null ? null : body.path("data");
        if (data == null || data.isMissingNode() || data.isNull() || data.path("name").asText("").isBlank()) {
            throw new CustomerException("Không tìm thấy doanh nghiệp với mã số thuế " + taxCode,
                    CustomerErrorCode.INVALID_ID);
        }

        TaxLookupDto dto = new TaxLookupDto(
                data.path("id").asText(taxCode),
                data.path("name").asText(null),
                data.path("shortName").asText(null),
                data.path("internationalName").asText(null),
                data.path("address").asText(null)
        );
        cache.put(taxCode, dto);
        return dto;
    }
}
