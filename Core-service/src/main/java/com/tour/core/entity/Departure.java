package com.tour.core.entity;
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

    // Thông tin điểm đón 
    @Column(name = "pickup_name")
    private String pickupName;

    @Column(name = "pickup_address", columnDefinition = "text")
    private String pickupAddress;

    @Column(name = "pickup_latitude")
    private Double pickupLatitude;

    @Column(name = "pickup_longitude")
    private Double pickupLongitude;

    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;

    private String status;

    @OneToOne(mappedBy = "departure", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private PriceConfig priceConfig;

}