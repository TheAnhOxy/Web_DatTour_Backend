package com.tour.identity.repository;

import com.tour.identity.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByPhone(@NotBlank(message = "PHONE_REQUIRED") @Pattern(regexp = "^\\d{10}$", message = "INVALID_PHONE_FORMAT") String phone);

    boolean existsByEmail(@NotBlank(message = "EMAIL_REQUIRED") @Email(message = "INVALID_EMAIL") String email);

    List<User> findAllByStatusNot(String status);

}