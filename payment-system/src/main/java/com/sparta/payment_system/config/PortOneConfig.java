package com.sparta.payment_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PortOneConfig {
    
    @Value("${portone.api.secret}")
    private String apiSecret;
    
    @Value("${portone.api.url}")
    private String apiUrl;
    
    @Bean("portOneWebClient")
    public WebClient portOneWebClient() {
        return WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "PortOne " + apiSecret)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
    
    public String getApiSecret() {
        return apiSecret;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
}
