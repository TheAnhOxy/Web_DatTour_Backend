package com.tour.core.repository;

import com.tour.core.entity.Destination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinationRepository extends JpaRepository<Destination, Long> {

    Page<Destination> findByCityNameContainingIgnoreCaseOrRegionContainingIgnoreCaseOrCountryContainingIgnoreCase(
            String cityName, String region, String country, Pageable pageable);
}
