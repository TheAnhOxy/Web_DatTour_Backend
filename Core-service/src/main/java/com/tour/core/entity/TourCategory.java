package com.tour.core.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tour_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TourCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
}
