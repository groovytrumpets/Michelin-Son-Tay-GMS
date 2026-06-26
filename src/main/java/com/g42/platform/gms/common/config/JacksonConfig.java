package com.g42.platform.gms.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Cấu hình Jackson toàn cục:
 * - Coerce empty string "" sang null cho các enum field
 *   (tránh HttpMessageNotReadableException khi FE gửi "" thay vì null)
 * - Không fail khi gặp property không biết (FAIL_ON_UNKNOWN_PROPERTIES = false)
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.createXmlMapper(false).build();

        // Cho phép coerce "" -> null cho enum
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false);
        mapper.configure(MapperFeature.ALLOW_COERCION_OF_SCALARS, true);

        // Coerce empty string thành null cho enum và các type khác
        mapper.coercionConfigDefaults()
              .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.EmptyString,
                           com.fasterxml.jackson.databind.cfg.CoercionAction.AsNull);

        return mapper;
    }
}
