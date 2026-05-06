package com.tour.core.repository;

import com.tour.core.entity.TourCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TourCategoryRepository extends JpaRepository<TourCategory, Long> {

    boolean existsByName(String name);

    @Query("SELECT c FROM Tour t RIGHT JOIN t.category c GROUP BY c ORDER BY COUNT(t) DESC")
    List<TourCategory> findTopCategories(Pageable pageable);
}