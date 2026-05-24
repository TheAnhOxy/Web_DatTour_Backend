package com.tour.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingNoteDTO {
    
    @JsonProperty("noteId")
    private Long noteId;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}
