package com.tour.booking.service.impl;

import com.tour.booking.client.CoreClient;
import com.tour.booking.dto.PassengerDTO;
import com.tour.booking.dto.request.BookingRequest;
import com.tour.booking.dto.response.ApiResponse;
import com.tour.booking.dto.response.BookingResponse;
import com.tour.booking.entity.Booking;
import com.tour.booking.entity.Passenger;
import com.tour.booking.exception.BusinessException;
import com.tour.booking.repository.BookingRepository;
import com.tour.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RedissonClient redissonClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CoreClient coreClient;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        // 1. Lấy dữ liệu từ Core Service TRƯỚC (Để vừa nạp Redis vừa tính tiền)
        ApiResponse coreResponse = coreClient.getDepartureDetails(request.getDepartureId());

        if (coreResponse == null || coreResponse.getStatus() != 200 || coreResponse.getData() == null) {
            throw new BusinessException("Không tìm thấy thông tin lịch khởi hành từ Core Service!");
        }

        // Ép kiểu data về Map để truy xuất dễ dàng
        Map<String, Object> departureData = (Map<String, Object>) coreResponse.getData();

        // 2. Xử lý Slot trong Redis (Check & Sync)
        String slotKey = "SLOTS_" + request.getDepartureId();
        RBucket<Integer> bucket = redissonClient.getBucket(slotKey);
        Integer availableSlots = bucket.get();

        if (availableSlots == null) {
            log.info("Redis chưa có slot, tiến hành nạp từ dữ liệu Core Service");
            int max = (int) departureData.get("maxSlots");
            int booked = departureData.get("bookedSlots") != null ? (int) departureData.get("bookedSlots") : 0;
            availableSlots = max - booked;
            bucket.set(availableSlots);
        }

        if (availableSlots < request.getPassengers().size()) {
            throw new BusinessException("Xin lỗi tiền bối, tour này đã hết chỗ!");
        }

        // 3. Trích xuất PriceConfig từ trong departureData
        Map<String, Object> priceConfig = (Map<String, Object>) departureData.get("priceConfig");
        if (priceConfig == null) {
            throw new BusinessException("Lịch khởi hành này chưa được cấu hình giá, không thể đặt chỗ!");
        }

        // 4. Tính toán tổng tiền
        BigDecimal totalAmount = calculateTotal(request.getPassengers(), priceConfig);

        // 5. Tạo Booking Entity (Snapshot toàn bộ thông tin quan trọng)
        Booking booking = Booking.builder()
                .bookingCode("BK" + System.nanoTime())
                .userId(request.getUserId())
                .departureId(request.getDepartureId())
                .status("PENDING")
                .totalAmount(totalAmount)
                .priceSnapshot(Map.of(
                        "tourTitle", departureData.get("tourTitle") != null ? departureData.get("tourTitle") : "N/A",
                        "startDate", departureData.get("startDate") != null ? departureData.get("startDate") : "",
                        "pickupAddress", departureData.get("pickupAddress") != null ? departureData.get("pickupAddress") : "",
                        "priceConfig", priceConfig
                ))
                .build();

        // 6. Map hành khách
        List<Passenger> passengers = request.getPassengers().stream()
                .map(p -> Passenger.builder()
                        .fullName(p.getFullName())
                        .dob(p.getDob())
                        .gender(p.getGender())
                        .ageGroup(p.getAgeGroup())
                        .idCardNumber(p.getIdCardNumber())
                        .booking(booking)
                        .build())
                .toList();
        booking.setPassengers(passengers);

        // 7. Cập nhật Redis (Trừ slot)
        bucket.set(availableSlots - passengers.size());

        // 8. Lưu vào Database & Bắn Kafka
        Booking saved = bookingRepository.save(booking);
        try {
            kafkaTemplate.send("booking-created-topic", saved.getBookingCode());
            log.info("Đã bắn event tạo booking cho code: {}", saved.getBookingCode());
        } catch (Exception e) {
            // Nếu Kafka lỗi, ta chỉ log lại chứ không làm hỏng cả request của khách
            log.error("Không thể gửi event lên Kafka: {}", e.getMessage());
        }

        return BookingResponse.builder()
                .bookingCode(saved.getBookingCode())
                .status(saved.getStatus())
                .totalAmount(saved.getTotalAmount())
                .createdAt(saved.getCreatedAt())
                .message("Đặt chỗ thành công! Vui lòng thanh toán trong vòng 10 phút.")
                .build();
    }

    private BigDecimal calculateTotal(List<PassengerDTO> passengers, Map<String, Object> priceConfig) {
        BigDecimal total = BigDecimal.ZERO;

        BigDecimal adultP = new BigDecimal(priceConfig.get("adultPrice").toString());
        BigDecimal c1014P = new BigDecimal(priceConfig.get("child1014Price").toString());
        BigDecimal c49P = new BigDecimal(priceConfig.get("child49Price").toString());
        BigDecimal babyP = new BigDecimal(priceConfig.get("babyPrice").toString());

        for (PassengerDTO p : passengers) {
            switch (p.getAgeGroup()) {
                case "ADULT" -> total = total.add(adultP);
                case "CHILD_10_14" -> total = total.add(c1014P);
                case "CHILD_4_9" -> total = total.add(c49P);
                case "BABY" -> total = total.add(babyP);
            }
        }
        return total;
    }
}


