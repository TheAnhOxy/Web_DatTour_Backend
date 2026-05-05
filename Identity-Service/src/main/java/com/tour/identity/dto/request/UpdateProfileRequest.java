package com.tour.identity.dto.request;


import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateProfileRequest {
    private String fullName;
    private String phone;
    private String address;
    private LocalDate dob;
    private String gender;
    private String avatarUrl;
}