package com.tour.booking.service.impl;

import com.tour.booking.client.CoreClient;
import com.tour.booking.dto.PassengerDTO;
import com.tour.booking.dto.request.BookingRequest;
import com.tour.booking.dto.request.CancelBookingRequest;
import com.tour.booking.dto.response.ApiResponse;
import com.tour.booking.dto.response.BookingResponse;
import com.tour.booking.entity.Booking;
import com.tour.booking.entity.Cancellation;
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
import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        String lockKey = "LOCK_TOUR_" + request.getDepartureId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            //  Giữ Distributed Lock để chống Overbooking tuyệt đối
            boolean isLocked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException("Hệ thống đang bận xử lý lượt đặt chỗ này, vui lòng thử lại sau!");
            }

            //  Lấy dữ liệu từ Core Service
            ApiResponse coreResponse = coreClient.getDepartureDetails(request.getDepartureId());
            if (coreResponse == null || coreResponse.getStatus() != 200 || coreResponse.getData() == null) {
                throw new BusinessException("Không tìm thấy thông tin lịch khởi hành!");
            }
            Map<String, Object> departureData = (Map<String, Object>) coreResponse.getData();
            Map<String, Object> priceConfig = (Map<String, Object>) departureData.get("priceConfig");

            // Check Slot (Redis First)
            String slotKey = "SLOTS_" + request.getDepartureId();
            RBucket<Object> bucket = redissonClient.getBucket(slotKey);
            Object bucketValue = bucket.get();

            Integer availableSlots;

            if (bucketValue == null) {
                log.info("Redis chưa có slot, tiến hành sync từ Core Service");
                int max = Integer.parseInt(departureData.get("maxSlots").toString());

                int booked = departureData.get("bookedSlots") != null
                        ? Integer.parseInt(departureData.get("bookedSlots").toString())
                        : 0;

                availableSlots = max - booked;
                bucket.set(availableSlots);
            } else {
                availableSlots = Integer.parseInt(bucketValue.toString());
            }

            if (availableSlots < request.getPassengers().size()) {
                throw new BusinessException("Xin lỗi, tour này không còn đủ chỗ trống!");
            }

            //Tính tiền & Tạo Booking Entity
            BigDecimal totalAmount = calculateTotal(request.getPassengers(), priceConfig);

            Booking booking = Booking.builder()
                    .bookingCode("BK" + System.nanoTime())

                    .userId(request.getUserId())
                    .departureId(request.getDepartureId())
                    .status("PENDING")
                    .totalAmount(totalAmount)
                    .priceSnapshot(Map.of(
                            "tourTitle", departureData.get("tourTitle") != null ? departureData.get("tourTitle") : "N/A",
                            "startDate", departureData.get("startDate") != null ? departureData.get("startDate") : "",
                            "priceConfig", priceConfig
                    ))
                    .build();

            // Bước 5: Map Passengers & Update Slot
            List<Passenger> passengers = request.getPassengers().stream()
                    .map(p -> Passenger.builder()
                            .fullName(p.getFullName()).dob(p.getDob()).gender(p.getGender())
                            .ageGroup(p.getAgeGroup()).idCardNumber(p.getIdCardNumber()).booking(booking)
                            .build()).toList();
            booking.setPassengers(passengers);

            // Cập nhật Redis ngay lập tức để giữ chỗ
            bucket.set(availableSlots - passengers.size());

            //  Lưu DB & Gửi Kafka
            Booking saved = bookingRepository.save(booking);

            publishBookingCreatedEvent(saved);

            // Trả về chi tiết (Gọi hàm buildDetailedResponse)
            return buildDetailedResponse(saved, request.getPassengers(), priceConfig);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Lỗi gián đoạn khi đang xử lý giữ chỗ!");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private BookingResponse buildDetailedResponse(Booking saved, List<PassengerDTO> passengerDTOs, Map<String, Object> priceConfig) {
        StringBuilder breakdown = new StringBuilder();

        long adult = passengerDTOs.stream().filter(p -> "ADULT".equals(p.getAgeGroup())).count();
        long c1014 = passengerDTOs.stream().filter(p -> "CHILD_10_14".equals(p.getAgeGroup())).count();
        long c49 = passengerDTOs.stream().filter(p -> "CHILD_4_9".equals(p.getAgeGroup())).count();
        long baby = passengerDTOs.stream().filter(p -> "BABY".equals(p.getAgeGroup())).count();

        if (adult > 0) breakdown.append(String.format("Người lớn: %d x %s; ", adult, priceConfig.get("adultPrice")));
        if (c1014 > 0) breakdown.append(String.format("Trẻ em (10-14): %d x %s; ", c1014, priceConfig.get("child1014Price")));
        if (c49 > 0) breakdown.append(String.format("Trẻ em (4-9): %d x %s; ", c49, priceConfig.get("child49Price")));
        if (baby > 0) breakdown.append(String.format("Em bé: %d x %s; ", baby, priceConfig.get("babyPrice")));

        return BookingResponse.builder()
                .bookingCode(saved.getBookingCode())
                .status(saved.getStatus())
                .totalAmount(saved.getTotalAmount())
                .createdAt(saved.getCreatedAt())
                .tourTitle((String) saved.getPriceSnapshot().get("tourTitle"))
                .startDate(String.valueOf(saved.getPriceSnapshot().get("startDate")))
                .priceDetail(priceConfig)
                .message("Đặt chỗ thành công! Diễn giải: " + breakdown.toString().trim())
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

    private void publishBookingCreatedEvent(Booking booking) {
        try {
            // Đóng gói dữ liệu đầy đủ để Payment Service không bị thiếu ID
            Map<String, Object> message = Map.of(
                    "bookingId", booking.getId(),
                    "bookingCode", booking.getBookingCode(),
                    "totalAmount", booking.getTotalAmount()
            );

            kafkaTemplate.send("booking-created-topic", message)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("=> [Kafka] Đã gửi thông tin Booking: {}", booking.getBookingCode());
                        } else {
                            log.error("=> [Kafka] Lỗi gửi tin nhắn: {}", ex.getMessage());
                        }
                    });
        } catch (Exception ex) {
            log.warn("Kafka Error: {}", ex.getMessage());
        }
    }
    @Transactional
    @Override
    public void cancelBooking(CancelBookingRequest request) {
        Booking booking = bookingRepository.findByBookingCode(request.getBookingCode())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + request.getBookingCode()));

        // Kiểm tra trạng thái: Đã hủy rồi thì không hủy nữa
        if ("CANCELLED".equals(booking.getStatus()) || "CANCELLED_TIMEOUT".equals(booking.getStatus())) {
            throw new RuntimeException("Đơn hàng này đã được hủy trước đó!");
        }
        Cancellation cancellation = Cancellation.builder()
                .reason(request.getReason())
                .cancelledAt(LocalDateTime.now())
                .booking(booking)
                .build();

        booking.setCancellation(cancellation);
        booking.setStatus("CANCELLED");
        booking.setCancellation(cancellation);
        bookingRepository.save(booking);

        //  Hoàn trả Slot trên Redis
        String slotKey = "SLOTS_" + booking.getDepartureId();
        RBucket<Object> bucket = redissonClient.getBucket(slotKey);

        if (bucket.isExists()) {
            int currentSlots = Integer.parseInt(bucket.get().toString());
            int restoredSlots = currentSlots + booking.getPassengers().size();
            bucket.set(restoredSlots);
            log.info("=> [CANCEL] Đã hoàn trả {} chỗ. Slot hiện tại trên Redis: {}",
                    booking.getPassengers().size(), restoredSlots);
        }
    }

    @Override
    public BookingResponse getBookingByCode(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn hàng: " + bookingCode));

        //  Lấy lại priceConfig từ snapshot đã lưu trong DB
        Map<String, Object> priceConfig = (Map<String, Object>) booking.getPriceSnapshot().get("priceConfig");

        return BookingResponse.builder()
                .bookingCode(booking.getBookingCode())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .createdAt(booking.getCreatedAt())
                .tourTitle((String) booking.getPriceSnapshot().get("tourTitle"))
                .startDate(String.valueOf(booking.getPriceSnapshot().get("startDate")))
                .priceDetail(priceConfig)
                .message("Lấy thông tin đơn hàng thành công")
                .build();
    }
}