package com.tour.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "passengers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Passenger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    private String fullName;
    private LocalDate dob;
    private String gender;
    private String ageGroup; // ADULT, CHILD, BABY
    private String idCardNumber;
    private String passportNumber;
}