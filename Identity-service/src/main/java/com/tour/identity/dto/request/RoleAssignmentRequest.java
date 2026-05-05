package com.tour.identity.dto.request;

import lombok.Data;

import java.util.Set;

@Data
public class RoleAssignmentRequest {
    private Long userId;
    private Set<String> roleNames; // Danh sách tên Role muốn gán
}