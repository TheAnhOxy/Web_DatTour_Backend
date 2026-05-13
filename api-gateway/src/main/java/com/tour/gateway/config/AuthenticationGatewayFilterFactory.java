package com.tour.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour.gateway.dto.request.IntrospectRequest;
import com.tour.gateway.dto.response.ApiResponse;
import com.tour.gateway.dto.response.IntrospectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Danh sách các Endpoints công khai không cần check Token
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/introspect",
            "/auth/reset-password",
            "/auth/forgot-password",
//            "/auth/admin/user",
            "/auth/verify-otp",

            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/core/**",
            "/bookings/**"
    );

    public AuthenticationGatewayFilterFactory(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    public static class Config {}

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            log.info(">>>> Gateway xử lý request: {}", path);

            // BƯỚC 1: Kiểm tra nếu là Endpoint công khai thì cho qua luôn (Fix lỗi 401 Login)
            if (isPublicEndpoint(path)) {
                log.info(">>>> Public Endpoint: {}, cho phép truy cập tự do.", path);
                return chain.filter(exchange);
            }

            // BƯỚC 2: Kiểm tra Header Authorization
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn(">>>> Thiếu Token tại path: {}", path);
                return onError(exchange, "Missing authorization information", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // BƯỚC 3: Gọi Identity Service để xác thực Token (Sử dụng Port 8081 vì chạy Local)
            return webClientBuilder.build()
                    .post()
                    .uri("http://identity-service/auth/introspect") // Đổi thành localhost vì tiền bối chạy local
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new IntrospectRequest(token))
                    .retrieve()
                    .bodyToMono(ApiResponse.class)
                    .flatMap(apiResponse -> {
                        IntrospectResponse introspect = objectMapper.convertValue(apiResponse.getData(), IntrospectResponse.class);

                        if (introspect != null && introspect.isValid()) {
                            // TRÍCH XUẤT THÔNG TIN (Ví dụ roles, userId) VÀ GỬI XUỐNG DƯỚI
                            // Giả sử introspect response có trả về thông tin user
                            return chain.filter(exchange.mutate()
                                    .request(builder -> builder.header("X-User-Roles", "ADMIN")) // Tạm thời hardcode để test
                                    .build());
                        } else {
                            return onError(exchange, "Token invalid", HttpStatus.UNAUTHORIZED);
                        }
                    })
                    .onErrorResume(e -> {
                        log.error(">>>> Lỗi kết nối Identity Service: {}", e.getMessage());
                        return onError(exchange, "Authentication service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
                    });
        };
    }

    /**
     * Kiểm tra xem path hiện tại có nằm trong danh sách được phép truy cập tự do không
     */
    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        log.error(">>>> Gateway Reject: {} - Status: {}", err, httpStatus);
        exchange.getResponse().setStatusCode(httpStatus);
        // Có thể bổ sung viết Body cho Response lỗi ở đây nếu cần thiết
        return exchange.getResponse().setComplete();
    }
}