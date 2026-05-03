package com.tour.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="departures")
@Data
public class Departure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="tour_id")
    private Tour tour;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Integer maxSlots;
    private Integer bookedSlots;

    private String status;
}