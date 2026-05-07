package com.tour.booking.consumer;

import com.tour.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

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
}