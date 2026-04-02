package com.g42.platform.gms.hikvision.config;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

@Configuration
@EnableConfigurationProperties(HikvisionProperties.class)
public class HikvisionHttpClientConfig {

    @Bean
    public CloseableHttpClient hikvisionHttpClient(HikvisionProperties props) throws Exception {
        // Trust all certs - Hikvision devices use self-signed certificates
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(null, (chain, authType) -> true)
                .build();

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(new HttpHost("https", props.getHost(), props.getPort())),
                new UsernamePasswordCredentials(props.getUsername(), props.getPassword().toCharArray())
        );

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(props.getConnectTimeoutSeconds()))
                .setResponseTimeout(Timeout.ofSeconds(props.getSocketTimeoutSeconds()))
                .build();

        return HttpClientBuilder.create()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setSSLSocketFactory(
                                        SSLConnectionSocketFactoryBuilder.create()
                                                .setSslContext(sslContext)
                                                .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                                .build()
                                )
                                .build()
                )
                .setDefaultCredentialsProvider(credentialsProvider)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
