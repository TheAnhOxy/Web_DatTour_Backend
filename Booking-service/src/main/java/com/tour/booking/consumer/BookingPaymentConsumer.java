package com.tour.booking.consumer;

import com.tour.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingPaymentConsumer {
    private final BookingRepository bookingRepository;

    @KafkaListener(topics = "payment-completed-topic", groupId = "booking-group")
    public void handlePaymentSuccess(String message) {
        String bookingCode = message.replace("\"", "");
        log.info("=> [Booking Service] Đang xử lý xác nhận cho đơn: {}", bookingCode);

        bookingRepository.findByBookingCode(bookingCode).ifPresentOrElse(booking -> {
            booking.setStatus("CONFIRMED");
            bookingRepository.save(booking);
            log.info("=> CẬP NHẬT THÀNH CÔNG: {}", bookingCode);
        }, () -> log.error("=> KHÔNG TÌM THẤY đơn hàng: {}", bookingCode));
    }

    @KafkaListener(topics = "payment-timeout-topic", groupId = "booking-group")
    public void handlePaymentTimeout(Map<String, Object> message) {
        Object bookingIdRaw = message.get("bookingId");
        if (bookingIdRaw == null) {
            log.error("=> [Booking Service] Timeout event thiếu bookingId: {}", message);
            return;
        }

        Long bookingId = Long.valueOf(bookingIdRaw.toString());
        log.info("=> [Booking Service] Nhận timeout payment cho bookingId={}", bookingId);

        bookingRepository.findById(bookingId).ifPresentOrElse(booking -> {
            if ("PENDING".equals(booking.getStatus())) {
                booking.setStatus("CANCELLED");
                bookingRepository.save(booking);
                log.info("=> [Booking Service] Đã HỦY booking {} do hết thời gian thanh toán", bookingId);
            } else {
                log.info("=> [Booking Service] Bỏ qua timeout vì booking {} đang ở trạng thái: {}",
                        bookingId, booking.getStatus());
            }
        }, () -> log.error("=> [Booking Service] Không tìm thấy booking id={}", bookingId));
    }
}