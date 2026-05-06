package com.tour.booking.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PassengerDTO {
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String ageGroup;
    private String idCardNumber;
}