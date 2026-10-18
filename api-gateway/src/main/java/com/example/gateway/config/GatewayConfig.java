package com.example.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("document-service-cb", r -> r
                        .path("/api/documents/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("documentServiceCB")
                                        .setFallbackUri("forward:/fallback/document"))
                                .retry(config -> config.setRetries(2))
                        )
                        .uri("http://localhost:8082"))
                .build();
    }
}