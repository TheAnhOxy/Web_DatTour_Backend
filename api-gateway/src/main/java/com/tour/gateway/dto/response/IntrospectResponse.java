package com.tour.gateway.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntrospectResponse {
    private boolean valid;
    private Long userId;     // Phải trùng khớp với cấu trúc cục data mà Identity Service ném về
    private List<String> roles;
}