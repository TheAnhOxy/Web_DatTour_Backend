package com.tour.booking.dto.request;

import com.tour.booking.dto.PassengerDTO;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingRequest {

    @NotNull(message = "ID người dùng không được để trống")
    private Long userId;
    private Long departureId;
    private List<PassengerDTO> passengers;
    private String note;
    private String promotionCode;

    @NotBlank(message = "Tên người liên hệ không được để trống")
    @Size(max = 100, message = "Tên liên hệ không được vượt quá 100 ký tự")
    private String contactName;
    @NotBlank(message = "Email liên hệ không được để trống")
    @Email(message = "Định dạng Email không hợp lệ")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String contactEmail;

    @NotBlank(message = "Số điện thoại không được để trống")
//    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    private String contactPhone;
}