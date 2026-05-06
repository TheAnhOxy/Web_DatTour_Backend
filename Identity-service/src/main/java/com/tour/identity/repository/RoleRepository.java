package com.tour.identity.repository;

import com.tour.identity.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Set<Role> findByNameIn(Set<String> roleNames);
}
