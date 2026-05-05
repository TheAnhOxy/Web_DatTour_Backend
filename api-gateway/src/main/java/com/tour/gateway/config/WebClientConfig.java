package com.tour.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    @LoadBalanced // <--- QUAN TRỌNG: Giúp giải mã cái tên 'identity-service' thành IP:PORT
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
