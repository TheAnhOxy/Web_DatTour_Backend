package com.tour.core.repository;

import com.tour.core.entity.Departure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartureRepository extends JpaRepository<Departure, Long> {
    List<Departure> findByTourId(Long tourId);
    List<Departure> findByTourIdAndStatus(Long tourId, String status);

    Page<Departure> findByTourId(Long tourId, Pageable pageable);

    Page<Departure> findByTourIdAndStatus(Long tourId, String status, Pageable pageable);

    void deleteByTourId(Long tourId);
}
