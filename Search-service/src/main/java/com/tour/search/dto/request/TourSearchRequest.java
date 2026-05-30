package com.tour.search.dto.request;


import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TourSearchRequest {
    private String keyword;             // Tìm kiếm theo tên tour, điểm đến, địa danh
    private BigDecimal priceFrom;       // Giá từ (basePrice)
    private BigDecimal priceTo;         // Giá đến (basePrice)
    private LocalDate startDateFrom;    // Lịch khởi hành từ ngày
    private LocalDate startDateTo;      // Lịch khởi hành đến ngày
    private String categoryName;        // Thể loại tour (Khớp chính xác Keyword)
    private String transportationType;  // Loại phương tiện (Khớp chính xác Keyword)
    private String region;              // Vùng miền
}