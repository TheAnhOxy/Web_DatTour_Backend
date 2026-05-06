package com.tour.core.repository;

import com.tour.core.entity.PriceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriceConfigRepository extends JpaRepository<PriceConfig, Long> {
    Optional<PriceConfig> findByDepartureId(Long departureId);
}
