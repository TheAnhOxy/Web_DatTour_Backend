package com.tour.booking.service.impl;

import com.tour.booking.dto.request.BookingRequest;
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
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RedissonClient redissonClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        String lockKey = "LOCK_TOUR_" + request.getDepartureId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Wait 3s, Lease 10m
            if (lock.tryLock(3, 600, TimeUnit.SECONDS)) {
                String slotKey = "SLOTS_" + request.getDepartureId();
                RBucket<Integer> bucket = redissonClient.getBucket(slotKey);

                // Giả sử Core Service đã push số lượng chỗ vào Redis khi Tour tạo mới
                Integer availableSlots = bucket.get();
                if (availableSlots == null || availableSlots < request.getPassengers().size()) {
                    throw new BusinessException("Tour đã hết chỗ!");
                }

                // Atomic decrement trong Redis
                bucket.set(availableSlots - request.getPassengers().size());

                // Build Entity
                Booking booking = Booking.builder()
                        .bookingCode("BK" + System.nanoTime())
                        .userId(request.getUserId())
                        .departureId(request.getDepartureId())
                        .status("PENDING")
                        .totalAmount(new BigDecimal("5000000")) // for ex
                        .build();

                // core
// Trong BookingServiceImpl
//                private final CoreClient coreClient;

// ... bên trong hàm createBooking
//                ApiResponse coreResponse = coreClient.getDepartureDetail(request.getDepartureId());
//// Giả sử tiền bối lấy giá từ field 'price' trong data của coreResponse
//                Map<String, Object> departureData = (Map<String, Object>) coreResponse.getData();
//                BigDecimal pricePerPerson = new BigDecimal(departureData.get("price").toString());
//
//                BigDecimal total = pricePerPerson.multiply(BigDecimal.valueOf(request.getPassengers().size()));
//                booking.setTotalAmount(total);

                List<Passenger> passengers = request.getPassengers().stream()
                        .map(p -> Passenger.builder()
                                .fullName(p.getFullName())
                                .booking(booking)
                                .build())
                        .toList();
                booking.setPassengers(passengers);

                Booking saved = bookingRepository.save(booking);
                kafkaTemplate.send("booking-pending-topic", saved.getBookingCode());

                return BookingResponse.builder()
                        .bookingCode(saved.getBookingCode())
                        .status(saved.getStatus())
                        .totalAmount(saved.getTotalAmount())
                        .createdAt(saved.getCreatedAt())
                        .message("Giữ chỗ thành công trong 10 phút!")
                        .build();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
        throw new RuntimeException("Hệ thống đang bận, vui lòng thử lại sau!");
    }

}


