package com.tour.core.repository;

import com.tour.core.entity.Departure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartureRepository extends JpaRepository<Departure, Long> {
    List<Departure> findByTourId(Long tourId);
    void deleteByTourId(Long tourId);
}
