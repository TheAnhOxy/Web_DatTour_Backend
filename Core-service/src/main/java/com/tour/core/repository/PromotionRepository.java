package com.tour.core.repository;

import com.tour.core.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCode(String code);

    boolean existsByCode(String code);

    Page<Promotion> findByIsActive(Boolean isActive, Pageable pageable);
}
