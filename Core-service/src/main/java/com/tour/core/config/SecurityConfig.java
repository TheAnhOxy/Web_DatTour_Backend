package com.tour.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour.core.dto.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Configuration
public class SecurityConfig {

    @Value("${jwt.signerKey}")
    private String signerKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(request -> request

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll()

                        // ADMIN
                        .requestMatchers(HttpMethod.GET, "/core/tours/admin")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")

                        // PUBLIC APIs
                        .requestMatchers(HttpMethod.GET,
                   "/core/tours",
                                "/core/tours/*",
                                "/core/departures/**",
                                "/core/categories/**",
                                "/core/destinations/**"
                        ).permitAll()

                        // WISHLIST
                        .requestMatchers(HttpMethod.POST, "/core/wishlists/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF", "ROLE_CUSTOMER")

                        .requestMatchers(HttpMethod.DELETE, "/core/wishlists/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF", "ROLE_CUSTOMER")

                        .requestMatchers(HttpMethod.GET, "/core/wishlists/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF", "ROLE_CUSTOMER")

                        // WRITE APIs
                        .requestMatchers(HttpMethod.POST, "/core/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers(HttpMethod.PUT, "/core/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/core/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")

                        .anyRequest().authenticated()
                )
//                .authorizeHttpRequests(request -> request
//                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/**").permitAll()
//                    .requestMatchers(HttpMethod.GET, "/core/tours/admin").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
//                    // wishlist endpoints: customers may manage their wishlist
//                    .requestMatchers(HttpMethod.POST, "/core/wishlists/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF", "ROLE_CUSTOMER")
//                    .requestMatchers(HttpMethod.DELETE, "/core/wishlists/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF", "ROLE_CUSTOMER")
//                    .requestMatchers(HttpMethod.GET, "/core/wishlists/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF", "ROLE_CUSTOMER")
//                    .requestMatchers(HttpMethod.POST, "/core/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF", "ROLE_CUSTOMER")
//                    .requestMatchers(HttpMethod.PUT, "/core/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
//                    .requestMatchers(HttpMethod.DELETE, "/core/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
//                        .requestMatchers("/core/departures/**").permitAll()
//                    .anyRequest().authenticated()
//                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ApiResponse apiResponse = ApiResponse.builder()
                                    .status(401)
                                    .message("Chưa đăng nhập hoặc token không hợp lệ")
                                    .data(null)
                                    .build();
                            response.getWriter().write(new ObjectMapper().writeValueAsString(apiResponse));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ApiResponse apiResponse = ApiResponse.builder()
                                    .status(403)
                                    .message("Bạn không có quyền thực hiện thao tác này")
                                    .data(null)
                                    .build();
                            response.getWriter().write(new ObjectMapper().writeValueAsString(apiResponse));
                        })
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("scope");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey secretKey = new SecretKeySpec(signerKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }
}