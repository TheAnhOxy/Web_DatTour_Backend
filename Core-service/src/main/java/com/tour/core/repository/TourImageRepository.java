package com.tour.core.repository;

import com.tour.core.entity.TourImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourImageRepository extends JpaRepository<TourImage, Long> {

    List<TourImage> findByTourIdOrderBySortOrderAsc(Long tourId);

    void deleteByTourId(Long tourId);
}
