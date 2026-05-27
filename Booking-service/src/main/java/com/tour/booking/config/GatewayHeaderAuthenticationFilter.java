package com.tour.booking.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String rolesHeader = request.getHeader("X-User-Roles");

        // Log debug để tiền bối nhìn thấy chính xác Gateway đang bơm cái gì xuống
        log.info("=> [Booking Filter] Nhận Header - UserId: {}, Roles: {}", userId, rolesHeader);

        if (userId != null && rolesHeader != null && !rolesHeader.isBlank()) {
            try {
                // Làm sạch chuỗi role (Xóa bỏ dấu ngoặc vuông [ ] nếu Gateway tự động bọc chuỗi)
                String cleanRoles = rolesHeader.replace("[", "").replace("]", "").trim();

                List<SimpleGrantedAuthority> authorities = Arrays.stream(cleanRoles.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // Tạo đối tượng Authentication với đầy đủ Credentials và Authorities
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);

                // Đính kèm chi tiết Request (Bắt buộc phải có để Spring Security chấp nhận)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Nạp vào ngữ cảnh bảo mật hệ thống
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("=> [Booking Filter] Nạp quyền vào Spring Security thành công: {}", authorities);

            } catch (Exception e) {
                log.error("=> [Booking Filter] Lỗi khi nạp cấu trúc quyền từ Gateway: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}