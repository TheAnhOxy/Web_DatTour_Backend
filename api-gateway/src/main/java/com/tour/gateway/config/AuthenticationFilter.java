package com.tour.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour.gateway.dto.response.ApiResponse;
import com.tour.gateway.dto.request.IntrospectRequest;
import com.tour.gateway.dto.response.IntrospectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper; // Dùng để map dữ liệu an toàn

    public AuthenticationFilter(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    public static class Config {}

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Missing authorization information", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            return webClientBuilder.build()
                    .post()
                    .uri("http://identity-service/auth/introspect")
                    .bodyValue(new IntrospectRequest(token))
                    .retrieve()
                    .bodyToMono(ApiResponse.class)
                    .flatMap(apiResponse -> {
                        // Ép kiểu an toàn từ LinkedHashMap sang IntrospectResponse
                        IntrospectResponse introspect = objectMapper.convertValue(
                                apiResponse.getData(),
                                IntrospectResponse.class
                        );

                        if (introspect != null && introspect.isValid()) {
                            return chain.filter(exchange);
                        } else {
                            return onError(exchange, "Token invalid or blacklisted", HttpStatus.UNAUTHORIZED);
                        }
                    })
                    .onErrorResume(e -> {
                        log.error("Authentication error: {}", e.getMessage());
                        return onError(exchange, "Authentication service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
                    });
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        log.error(err);
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }
}