package com.tour.identity.service.impl;

import com.tour.identity.dto.request.RoleAssignmentRequest;
import com.tour.identity.entity.Role;
import com.tour.identity.entity.User;
import com.tour.identity.repository.RoleRepository;
import com.tour.identity.repository.UserRepository;
import com.tour.identity.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void assignRoles(RoleAssignmentRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        Set<Role> roles = roleRepository.findByNameIn(request.getRoleNames());
        user.setRoles(roles);
        userRepository.save(user);
        // Bắn một Event qua Kafka thông báo "USER_UPDATED"
        // để các service khác cập nhật cache nếu cần.
        log.info("Roles updated for user: {}", user.getEmail());
    }
}