package com.cathay.coindesk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    private final Duration connectTimeout;
    private final Duration readTimeout;

    public RestTemplateConfig(@Value("${coindesk.api.connect-timeout-ms}") long connectTimeoutMs,
                              @Value("${coindesk.api.read-timeout-ms}") long readTimeoutMs) {
        this.connectTimeout = Duration.ofMillis(connectTimeoutMs);
        this.readTimeout = Duration.ofMillis(readTimeoutMs);
    }

    // 外部 API 一定要設 timeout，否則呼叫端執行緒會被上游拖住。
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }
}
