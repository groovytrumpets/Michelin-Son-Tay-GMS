package com.g42.platform.gms.hikvision.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.hikvision.config.HikvisionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Client để tạo user trên thiết bị Hikvision qua ISAPI.
 * Dùng cùng CloseableHttpClient với HikvisionApiClient (hỗ trợ Digest Auth).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HikvisionUserClient {

    private final CloseableHttpClient hikvisionHttpClient;
    private final HikvisionProperties properties;
    private final ObjectMapper objectMapper;

    public boolean createPerson(String employeeNo, String name) {
        if (!properties.isSyncEnabled()) {
            log.info("HikvisionUserClient: sync disabled, skip createPerson employeeNo={}", employeeNo);
            return false;
        }

        String url = "https://" + properties.getHost() + ":" + properties.getPort()
                + "/ISAPI/AccessControl/UserInfo/Record?format=json";

        try {
            Map<String, Object> body = Map.of(
                    "UserInfo", Map.of(
                            "employeeNo", employeeNo,
                            "name", name,
                            "userType", "normal",
                            "Valid", Map.of(
                                    "enable", true,
                                    "beginTime", "2024-01-01T00:00:00",
                                    "endTime", "2030-01-01T00:00:00"
                            )
                    )
            );
            String json = objectMapper.writeValueAsString(body);

            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            return hikvisionHttpClient.execute(request, response -> {
                int status = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                if (status >= 200 && status < 300) {
                    log.info("HikvisionUserClient: created person employeeNo={} name={}", employeeNo, name);
                    return true;
                } else {
                    log.warn("HikvisionUserClient: failed employeeNo={}, status={}, body={}", employeeNo, status, responseBody);
                    return false;
                }
            });

        } catch (Exception e) {
            log.error("HikvisionUserClient: error creating person employeeNo={}: {}", employeeNo, e.getMessage());
            return false;
        }
    }

    public boolean updatePerson(String employeeNo, String name) {
        if (!properties.isSyncEnabled()) return false;

        String url = "https://" + properties.getHost() + ":" + properties.getPort()
                + "/ISAPI/AccessControl/UserInfo/Modify?format=json";
        try {
            Map<String, Object> body = Map.of(
                    "UserInfo", Map.of(
                            "employeeNo", employeeNo,
                            "name", name,
                            "userType", "normal",
                            "Valid", Map.of(
                                    "enable", true,
                                    "beginTime", "2024-01-01T00:00:00",
                                    "endTime", "2035-01-01T00:00:00"
                            )
                    )
            );
            String json = objectMapper.writeValueAsString(body);
            HttpPut request = new HttpPut(url);
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            return hikvisionHttpClient.execute(request, response -> {
                int status = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                if (status >= 200 && status < 300) {
                    log.info("HikvisionUserClient: updated person employeeNo={}", employeeNo);
                    return true;
                }
                log.warn("HikvisionUserClient: update failed employeeNo={}, status={}, body={}", employeeNo, status, responseBody);
                return false;
            });
        } catch (Exception e) {
            log.error("HikvisionUserClient: error updating person employeeNo={}: {}", employeeNo, e.getMessage());
            return false;
        }
    }

    public boolean deletePerson(String employeeNo) {
        if (!properties.isSyncEnabled()) return false;

        String url = "https://" + properties.getHost() + ":" + properties.getPort()
                + "/ISAPI/AccessControl/UserInfo/Delete?format=json";
        try {
            Map<String, Object> body = Map.of(
                    "UserInfoDelCond", Map.of(
                            "EmployeeNoList", List.of(
                                    Map.of("employeeNo", employeeNo)
                            )
                    )
            );
            String json = objectMapper.writeValueAsString(body);
            HttpPut request = new HttpPut(url);
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            return hikvisionHttpClient.execute(request, response -> {
                int status = response.getCode();
                if (status >= 200 && status < 300) {
                    log.info("HikvisionUserClient: deleted person employeeNo={}", employeeNo);
                    return true;
                }
                log.warn("HikvisionUserClient: delete failed employeeNo={}, status={}", employeeNo, status);
                return false;
            });
        } catch (Exception e) {
            log.error("HikvisionUserClient: error deleting person employeeNo={}: {}", employeeNo, e.getMessage());
            return false;
        }
    }
}
