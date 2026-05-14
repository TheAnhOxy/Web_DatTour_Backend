package com.tour.booking.dto.response;

import com.tour.booking.dto.PassengerDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private String bookingCode;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private Long userId;
    private String message;
    private String tourTitle;
    private String startDate;

    private String cityName;
    private String pickupName;
    private String pickupAddress;
    private LocalDateTime pickupTime;

    private Map<String, Object> priceDetail;
    private List<PassengerDTO> passengers;
    private Map<String, Object> destination;
}
