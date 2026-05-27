package com.tour.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF vì đây là REST API dựa trên Token/Header nội bộ
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Cấu hình các API Stateless (Không duy trì Session)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. Phân quyền Endpoint
                .authorizeHttpRequests(auth -> auth
                        // CÁC API BẮT BUỘC QUYỀN ADMIN (Gồm /bookings/admin/** và các API hành khách của admin)
                        .requestMatchers("/bookings/admin/**").hasRole("ADMIN")
                        .requestMatchers("/bookings/passenger/**").hasRole("ADMIN")

                        // TẤT CẢ CÁC API CÒN LẠI ĐỀU CHO PHÉP TRUY CẬP TỰ DO (PermitAll)
                        .anyRequest().permitAll()
                );

        // 4. Giữ filter đọc Header từ API Gateway để bóc tách thông tin User/Admin khi cần
        http.addFilterBefore(new GatewayHeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}