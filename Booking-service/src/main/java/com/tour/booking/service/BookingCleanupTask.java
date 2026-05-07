package com.tour.booking.service;

import com.tour.booking.entity.Booking;
import com.tour.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupTask {

    private final BookingRepository bookingRepository;
    private final RedissonClient redissonClient;

    // Chạy định kỳ mỗi 30 giây
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES) // 10p
//    @Scheduled(fixedRate = 30000)
    @Transactional
    public void handleTimeoutBookings() {
        // Mốc thời gian: Hiện tại trừ đi 1 phút
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(1);

        // Tìm các booking vẫn đang PENDING mà đã quá 1 phút
        List<Booking> expiredBookings = bookingRepository
                .findByStatusAndCreatedAtBefore("PENDING", timeoutThreshold);

        if (expiredBookings.isEmpty()) return;

        log.info("Phát hiện {} đơn hàng hết hạn thanh toán", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            try {
                // Cập nhật trạng thái thành CANCELLED
                booking.setStatus("CANCELLED_TIMEOUT");
                bookingRepository.save(booking);

                // Trả lại slot vào Redis
                String slotKey = "SLOTS_" + booking.getDepartureId();

                RBucket<Object> bucket = redissonClient.getBucket(slotKey);
                if (bucket.isExists() && bucket.get() != null) {

                    int currentSlots = Integer.parseInt(bucket.get().toString());

                    int restoredSlots = currentSlots + booking.getPassengers().size();
                    bucket.set(restoredSlots);

                    log.info("=> Đã hủy đơn {}. Trả lại {} chỗ. Redis hiện tại: {}",
                            booking.getBookingCode(),
                            booking.getPassengers().size(),
                            restoredSlots);
                }
            } catch (Exception e) {
                log.error("Lỗi khi xử lý hủy đơn {}: {}", booking.getBookingCode(), e.getMessage());
            }
        }
    }
}