package com.tour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tours")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer durationDays;

    private String status; // active

    @Builder.Default
    private Boolean isHot = false;

    private BigDecimal basePrice;

    private Integer bookedSlots = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private TourCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transportation_id")
    private Transportation transportation;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL)
    private List<TourImage> images;

    @ManyToMany
    @JoinTable(
            name = "tour_destinations",
            joinColumns = @JoinColumn(name = "tour_id"),
            inverseJoinColumns = @JoinColumn(name = "destination_id")
    )
    private Set<Destination> destinations;

    @OneToMany(mappedBy = "tour")
    private List<Departure> departures;
    private LocalDateTime createdAt;


    @ManyToMany
    @JoinTable(
            name = "tour_promotions",
            joinColumns = @JoinColumn(name = "tour_id"),
            inverseJoinColumns = @JoinColumn(name = "promotion_id")
    )
    private Set<Promotion> promotions;

}
