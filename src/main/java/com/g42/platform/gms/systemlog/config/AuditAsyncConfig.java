package com.g42.platform.gms.systemlog.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Executor riêng cho ghi audit: queue đầy thì bỏ bản ghi (kèm warn) thay vì
 * chặn thread nghiệp vụ.
 */
@Slf4j
@Configuration
@EnableAsync
public class AuditAsyncConfig {

    @Bean(name = "auditExecutor")
    public ThreadPoolTaskExecutor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler((Runnable r, ThreadPoolExecutor pool) ->
                log.warn("Audit queue full — audit record dropped"));
        executor.initialize();
        return executor;
    }
}
