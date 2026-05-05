package com.tour.dto.request;


import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterRequest {

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "INVALID_EMAIL")
    private String email;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, message = "PASSWORD_TOO_SHORT")
    private String password;

    @NotBlank(message = "FULL_NAME_REQUIRED")
    private String fullName;

    @NotBlank(message = "PHONE_REQUIRED")
    @Pattern(regexp = "^\\d{10}$", message = "INVALID_PHONE_FORMAT")
    private String phone;

    private String address;
    private LocalDate dob;
    private String gender;
}