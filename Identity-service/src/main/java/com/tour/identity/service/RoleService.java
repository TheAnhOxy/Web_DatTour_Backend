package com.tour.identity.service;

import com.tour.identity.dto.request.RoleAssignmentRequest;

public interface RoleService {
    void assignRoles(RoleAssignmentRequest request);
}
