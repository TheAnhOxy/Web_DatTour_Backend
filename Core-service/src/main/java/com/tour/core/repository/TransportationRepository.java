package com.tour.core.repository;

import com.tour.core.entity.Transportation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportationRepository extends JpaRepository<Transportation, Long> {

    boolean existsByType(String type);
}
