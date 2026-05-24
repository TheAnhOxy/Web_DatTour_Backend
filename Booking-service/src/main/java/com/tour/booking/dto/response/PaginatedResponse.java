package com.tour.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {
    
    @JsonProperty("data")
    private List<T> data;
    
    @JsonProperty("totalElements")
    private Integer totalElements;
    
    @JsonProperty("currentPage")
    private Integer currentPage;
    
    @JsonProperty("pageSize")
    private Integer pageSize;
    
    @JsonProperty("totalPages")
    private Integer totalPages;
    
    @JsonProperty("hasNext")
    private Boolean hasNext;
}
