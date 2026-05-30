package com.tour.core.event;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TourEvent {
    private String status; // Ví dụ: "TOUR_CREATED"
    private Object tour;   // Hoặc dùng trực tiếp kiểu dữ liệu Tour của tiền bối
}
