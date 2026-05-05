package com.tour.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private LocalDate dob;
    private String gender;
    private String status;
    private String avatarUrl;
    private Integer currentPoints;
    private Set<String> roles;
}