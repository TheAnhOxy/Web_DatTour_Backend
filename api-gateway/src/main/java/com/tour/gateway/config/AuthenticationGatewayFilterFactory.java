package com.tour.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "**/auth/login",
            "**/auth/register",
            "**/auth/introspect",
            "**/auth/reset-password",
            "**/auth/forgot-password",
            "**/auth/verify-otp",

            "**/v3/api-docs/**",
            "**/swagger-ui/**",
            "**/swagger-resources/**",
            "**/core/**",

            // ===== BOOKING PUBLIC ENDPOINTS =====

            // Create / Cancel
            "**/bookings/create",
            "**/bookings/cancel",

            // User bookings
            "**/bookings/user/**",

            // Booking details
            "**/bookings/id/**",

            // Booking passengers / notes / cancellation
            "**/bookings/*/passengers",
            "**/bookings/*/notes",
            "**/bookings/*/cancellation",

            // Legacy endpoints
            "**/bookings/by-users",
            "**/bookings/by-ids",
            "**/bookings/batch",

            // Payment webhooks
            "**/payments/sepay-webhook",
            "**/payments/stripe-webhook",
            "**/payments/callback"
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
                            try {
                                // 1. Tự lực parse chuỗi Token gốc
                                SignedJWT signedJWT = SignedJWT.parse(token);

                                // 2. Đọc chính xác trường "userId" (Trường này khớp hoàn toàn)
                                String userId = signedJWT.getJWTClaimsSet().getClaim("userId").toString();

                                // 3. SỬA TẠI ĐÂY: Đọc chính xác trường "scope" dạng String của tiền bối
                                String scope = signedJWT.getJWTClaimsSet().getStringClaim("scope"); // Kết quả ví dụ: "ROLE_ADMIN USER_READ"

                                String roles = "USER"; // Quyền mặc định nếu trống

                                if (scope != null && !scope.isBlank()) {
                                    // Tách chuỗi bằng dấu cách thành các phần tử đơn lẻ
                                    List<String> authorities = Arrays.asList(scope.split(" "));

                                    // Lọc ra những chuỗi nào bắt đầu bằng "ROLE_" để gom cụm lại gửi sang Booking
                                    roles = authorities.stream()
                                            .filter(auth -> auth.startsWith("ROLE_"))
                                            // Cắt bỏ chữ "ROLE_" nếu muốn Booking tự thêm, hoặc giữ nguyên (Filter Booking của tiền bối tự clean được)
                                            .map(role -> role.replace("ROLE_", ""))
                                            .collect(Collectors.joining(",")); // Gộp thành dạng "ADMIN,USER"
                                }

                                log.info(">>>> [Gateway Trích Xuất Thành Công] UserId: {}, Roles gửi đi: {}", userId, roles);

                                // 4. Gán chặt vào Header chuyển tiếp downstream
                                return chain.filter(exchange.mutate()
                                        .request(exchange.getRequest().mutate()
                                                .header("X-User-Id", userId)
                                                .header("X-User-Roles", roles)
                                                .build())
                                        .build());

                            } catch (Exception e) {
                                log.error(">>>> Lỗi parse Token trực tiếp tại Gateway: {}", e.getMessage());
                                return onError(exchange, "Token claims invalid", HttpStatus.UNAUTHORIZED);
                            }
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