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
import java.util.stream.Collectors;

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
            boolean isLocked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException("Hệ hệ thống đang bận xử lý, vui lòng thử lại sau!");
            }

            // 1. Gọi Core Service lấy dữ liệu DTO mới
            ApiResponse coreResponse = coreClient.getDepartureDetails(request.getDepartureId());
            if (coreResponse == null || coreResponse.getStatus() != 200 || coreResponse.getData() == null) {
                throw new BusinessException("Không tìm thấy thông tin lịch khởi hành!");
            }

            // 2. Bóc tách Map (Jackson tự động map từ DepartureResponseBookingDTO sang Map này)
            Map<String, Object> departureData = (Map<String, Object>) coreResponse.getData();

            // Bóc tách các Object con đã được định nghĩa trong DTO mới của Core
            Map<String, Object> destData = (Map<String, Object>) departureData.get("destination");
            Map<String, Object> priceConfig = (Map<String, Object>) departureData.get("priceConfig");
            String tourTitle = departureData.get("tourTitle") != null ? departureData.get("tourTitle").toString() : "N/A";

            // 3. Kiểm tra Slot (Redis)
            String slotKey = "SLOTS_" + request.getDepartureId();
            RBucket<Object> bucket = redissonClient.getBucket(slotKey);
            Object bucketValue = bucket.get();
            Integer availableSlots;

            if (bucketValue == null) {
                int max = Integer.parseInt(departureData.get("maxSlots").toString());
                int booked = departureData.get("bookedSlots") != null ? Integer.parseInt(departureData.get("bookedSlots").toString()) : 0;
                availableSlots = max - booked;
                bucket.set(availableSlots);
            } else {
                availableSlots = Integer.parseInt(bucketValue.toString());
            }

            if (availableSlots < request.getPassengers().size()) {
                throw new BusinessException("Xin lỗi, tour này không còn đủ chỗ trống!");
            }

            // 4. Tính tiền & Tạo Snapshot
            BigDecimal totalAmount = calculateTotal(request.getPassengers(), priceConfig);

            Booking booking = Booking.builder()
                    .bookingCode("BK" + System.nanoTime())
                    .userId(request.getUserId())
                    .departureId(request.getDepartureId())
                    .status("PENDING")
                    .totalAmount(totalAmount)
                    .contactName(request.getContactName())
                    .contactEmail(request.getContactEmail())
                    .contactPhone(request.getContactPhone())
                    .priceSnapshot(Map.of(
                            "tourTitle", tourTitle,
                            "startDate", departureData.get("startDate") != null ? departureData.get("startDate").toString() : "",
                            "priceConfig", priceConfig != null ? priceConfig : Map.of(),

                            // LƯU NGUYÊN OBJECT DESTINATION TỪ CORE SANG
                            "destination", destData != null ? destData : Map.of(),

                            // Để FE dùng cityName nhanh hơn
                            "cityName", (destData != null && destData.get("cityName") != null) ? destData.get("cityName") : "N/A",

                            "pickupName", departureData.get("pickupName") != null ? departureData.get("pickupName") : "N/A",
                            "pickupAddress", departureData.get("pickupAddress") != null ? departureData.get("pickupAddress") : "N/A",
                            "pickupTime", departureData.get("pickupTime") != null ? departureData.get("pickupTime").toString() : ""
                    ))
                    .build();

            // 5. Lưu hành khách & Cập nhật Redis
            List<Passenger> passengers = request.getPassengers().stream()
                    .map(p -> Passenger.builder()
                            .fullName(p.getFullName()).dob(p.getDob()).gender(p.getGender())
                            .ageGroup(p.getAgeGroup()).idCardNumber(p.getIdCardNumber()).booking(booking)
                            .build()).toList();
            booking.setPassengers(passengers);

            bucket.set(availableSlots - passengers.size());

            Booking saved = bookingRepository.save(booking);
            publishBookingCreatedEvent(saved);

            return buildDetailedResponse(saved, request.getPassengers(), priceConfig);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Lỗi hệ thống khi giữ chỗ!");
        } finally {
            if (lock.isHeldByCurrentThread() && lock.isLocked()) {
                lock.unlock();
            }
        }
    }

    private BookingResponse buildDetailedResponse(Booking saved, List<PassengerDTO> passengerDTOs, Map<String, Object> priceConfig) {
        StringBuilder breakdown = new StringBuilder();
        Map<String, Object> priceSnapshot = saved.getPriceSnapshot();

        // Logic tính toán breakdown giữ nguyên
        long adult = passengerDTOs.stream().filter(p -> "ADULT".equals(p.getAgeGroup())).count();
        long c1014 = passengerDTOs.stream().filter(p -> "CHILD_10_14".equals(p.getAgeGroup())).count();
        long c49 = passengerDTOs.stream().filter(p -> "CHILD_4_9".equals(p.getAgeGroup())).count();
        long baby = passengerDTOs.stream().filter(p -> "BABY".equals(p.getAgeGroup())).count();

        if (adult > 0) breakdown.append(String.format("Người lớn: %d x %s; ", adult, priceConfig.get("adultPrice")));
        if (c1014 > 0) breakdown.append(String.format("Trẻ em (10-14): %d x %s; ", c1014, priceConfig.get("child1014Price")));
        if (c49 > 0) breakdown.append(String.format("Trẻ em (4-9): %d x %s; ", c49, priceConfig.get("child49Price")));
        if (baby > 0) breakdown.append(String.format("Em bé: %d x %s; ", baby, priceConfig.get("babyPrice")));

        // Xử lý an toàn chuỗi pickupTime trước khi build
        Object rawPickupTime = priceSnapshot.get("pickupTime");
        LocalDateTime parsedPickupTime = (rawPickupTime != null && !rawPickupTime.toString().isEmpty() && !rawPickupTime.toString().equals("null"))
                ? LocalDateTime.parse(rawPickupTime.toString())
                : null;

        return BookingResponse.builder()
                .bookingId(saved.getId())
                .bookingCode(saved.getBookingCode())
                .status(saved.getStatus())
                .totalAmount(saved.getTotalAmount())
                .createdAt(saved.getCreatedAt())
                .passengers(passengerDTOs)
                .tourTitle((String) priceSnapshot.get("tourTitle"))
                .startDate(String.valueOf(priceSnapshot.get("startDate")))
                .cityName((String) priceSnapshot.get("cityName"))
                .pickupName((String) priceSnapshot.get("pickupName"))
                .pickupAddress((String) priceSnapshot.get("pickupAddress"))
                .destination((Map<String, Object>) priceSnapshot.get("destination"))
                // ĐƯA VÀO ĐÂY: Sử dụng biến đã parse ở trên
                .pickupTime(parsedPickupTime)
                .priceDetail(priceConfig)
                .userId(saved.getUserId())
                .message("Giữ chỗ thành công! Diễn giải: " + breakdown.toString().trim())
                .build();
    }

    private BigDecimal calculateTotal(List<PassengerDTO> passengers, Map<String, Object> priceConfig) {
        // Nếu priceConfig null, báo lỗi nghiệp vụ rõ ràng thay vì để hệ thống crash 500
        if (priceConfig == null) {
            throw new BusinessException("Lỗi: Tour này chưa được cấu hình bảng giá. Vui lòng liên hệ admin!");
        }

        BigDecimal total = BigDecimal.ZERO;
        // Dùng hàm helper để lấy giá an toàn, tránh lỗi null pointer
        BigDecimal adultP = getPrice(priceConfig, "adultPrice");
        BigDecimal c1014P = getPrice(priceConfig, "child1014Price");
        BigDecimal c49P = getPrice(priceConfig, "child49Price");
        BigDecimal babyP = getPrice(priceConfig, "babyPrice");

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

    // Hàm bổ trợ lấy giá an toàn
    private BigDecimal getPrice(Map<String, Object> priceConfig, String key) {
        Object value = priceConfig.get(key);
        return value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
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
                .bookingId(booking.getId())
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

    @Override
    public List<BookingResponse> getBookingsByUserId(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserIdWithPassengers(userId);
        return bookings.stream()
                .map(this::mapToBookingResponse)
                .toList();
    }

    @Override
    public List<BookingResponse> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAllWithPassengers();
        return bookings.stream()
                .map(this::mapToBookingResponse)
                .toList();
    }

    // Hàm Helper để map dữ liệu sang DTO cho gọn code
    private BookingResponse mapToBookingResponse(Booking booking) {
        Map<String, Object> priceSnapshot = booking.getPriceSnapshot();
        Map<String, Object> priceConfig = (Map<String, Object>) priceSnapshot.get("priceConfig");
        List<PassengerDTO> passengerDTOs = booking.getPassengers().stream()
                .map(p -> PassengerDTO.builder()
                        .fullName(p.getFullName())
                        .ageGroup(p.getAgeGroup())
                        .dob(p.getDob())
                        .gender(p.getGender())
                        .idCardNumber(p.getIdCardNumber())
                        .build())
                .toList();
        return BookingResponse.builder()
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .createdAt(booking.getCreatedAt())
                .passengers(passengerDTOs)

                // Lấy từ Snapshot (Dữ liệu quá khứ)
                .tourTitle(priceSnapshot != null ? (String) priceSnapshot.get("tourTitle") : "N/A")
                .startDate(priceSnapshot != null ? String.valueOf(priceSnapshot.get("startDate")) : "")
                .cityName(priceSnapshot != null ? (String) priceSnapshot.get("cityName") : "")
                .pickupName(priceSnapshot != null ? (String) priceSnapshot.get("pickupName") : "")
                .pickupAddress(priceSnapshot != null ? (String) priceSnapshot.get("pickupAddress") : "")

                .priceDetail(priceSnapshot != null ? (Map<String, Object>) priceSnapshot.get("priceConfig") : null)
                .userId(booking.getUserId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<BookingResponse>> getBookingsByUserIds(List<Long> userIds) {
        List<Booking> bookings = bookingRepository.findAllByUserIdsWithPassengers(userIds);

        return bookings.stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.groupingBy(
                        res -> bookings.stream()
                                .filter(b -> b.getBookingCode().equals(res.getBookingCode()))
                                .findFirst().get().getUserId()
                ));
    }


    @Override
    @Transactional(readOnly = true)
    public Map<Long, BookingResponse> getBookingsByIds(List<Long> ids) {
        List<Booking> bookings = bookingRepository.findAllByIdsWithPassengers(ids);
        return bookings.stream().collect(Collectors.toMap(
                Booking::getId,
                this::mapToBookingResponse
        ));
    }
}