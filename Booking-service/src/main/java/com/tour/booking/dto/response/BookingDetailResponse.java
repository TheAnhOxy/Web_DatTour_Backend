package com.tour.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class BookingDetailResponse {
    
    @JsonProperty("bookingId")
    private Long bookingId;
    
    @JsonProperty("bookingCode")
    private String bookingCode;
    
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("departureId")
    private Long departureId;
    
    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;
    
    @JsonProperty("paidAmount")
    private BigDecimal paidAmount;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("paymentMethod")
    private String paymentMethod;
    
    @JsonProperty("paymentDueAt")
    private LocalDateTime paymentDueAt;
    
    @JsonProperty("contactName")
    private String contactName;
    
    @JsonProperty("contactEmail")
    private String contactEmail;
    
    @JsonProperty("contactPhone")
    private String contactPhone;
    
    @JsonProperty("priceSnapshot")
    private Map<String, Object> priceSnapshot;
    
    @JsonProperty("promotionSnapshot")
    private Map<String, Object> promotionSnapshot;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    
    @JsonProperty("version")
    private Long version;
    
    @JsonProperty("passengers")
    private List<PassengerDTO> passengers;
    
    @JsonProperty("bookingNotes")
    private List<BookingNoteDTO> bookingNotes;
    
    @JsonProperty("cancellation")
    private CancellationDTO cancellation;
}
