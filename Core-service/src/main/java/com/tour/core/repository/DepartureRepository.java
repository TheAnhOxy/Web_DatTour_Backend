package com.tour.core.repository;

import com.tour.core.entity.Departure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DepartureRepository extends JpaRepository<Departure, Long> {
    List<Departure> findByTourId(Long tourId);
    List<Departure> findByTourIdAndStatus(Long tourId, String status);

    Page<Departure> findByTourId(Long tourId, Pageable pageable);

    Page<Departure> findByTourIdAndStatus(Long tourId, String status, Pageable pageable);

    void deleteByTourId(Long tourId);

    @Query("SELECT COUNT(d) FROM Departure d WHERE d.status = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT COALESCE(SUM(d.maxSlots),0), COALESCE(SUM(d.bookedSlots),0) FROM Departure d WHERE d.status = 'OPEN'")
    Object[] sumSlotsForOpenDepartures();

    @Query("""
        SELECT d FROM Departure d
        WHERE d.startDate >= CURRENT_TIMESTAMP AND d.status = 'OPEN'
        ORDER BY d.startDate ASC
        """)
    List<Departure> findUpcomingDepartures(Pageable pageable);

    @Query("""
        SELECT d FROM Departure d WHERE d.status = 'OPEN' AND d.maxSlots > 0
        AND (CAST(d.bookedSlots AS double) * 1.0 / CAST(d.maxSlots AS double)) >= 0.9
        ORDER BY d.startDate ASC
        """)
    List<Departure> findNearlyFullDepartures();
}
