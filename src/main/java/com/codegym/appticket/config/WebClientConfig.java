package com.codegym.appticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient ipApiWebClient() {
        return WebClient.builder()
                .baseUrl("http://ip-api.com")
                .build();
    }
}
