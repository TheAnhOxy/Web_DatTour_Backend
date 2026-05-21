package com.tour.booking.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour.booking.entity.Booking;
import com.tour.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Nhận xác nhận thanh toán tại quầy từ payment-service và gửi email
 * (cùng luồng notification-topic như SePay/Stripe qua BookingPaymentConsumer).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BookingOfficeReservationConsumer {

    private static final DateTimeFormatter DUE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BookingRepository bookingRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${booking.office.address:Phạm Văn chiêu, Phường 9, Gò Vấp, Hồ Chí Minh}")
    private String officeAddress;

    @Value("${booking.office.hours:T2–T7, 8:00–17:30}")
    private String officeHours;

    @Value("${booking.office.hotline:1900-xxxx}")
    private String officeHotline;

    @KafkaListener(topics = "booking-office-reserved-topic", groupId = "booking-group")
    public void handleOfficeReserved(String message) {
        log.info("=> [Booking] Nhận office-reserved: {}", message);
        try {
            Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<>() {});
            Object bookingIdRaw = payload.get("bookingId");
            if (bookingIdRaw == null) {
                log.warn("=> [Booking] office-reserved thiếu bookingId");
                return;
            }
            Long bookingId = Long.valueOf(bookingIdRaw.toString());
            String paymentDueAtRaw = payload.get("paymentDueAt") != null
                    ? payload.get("paymentDueAt").toString() : null;

            bookingRepository.findById(bookingId).ifPresentOrElse(booking -> {
                booking.setPaymentMethod("CASH_OFFICE");
                LocalDateTime paymentDueAt = null;
                if (paymentDueAtRaw != null && !paymentDueAtRaw.isBlank()) {
                    paymentDueAt = LocalDateTime.parse(paymentDueAtRaw);
                    booking.setPaymentDueAt(paymentDueAt);
                }
                bookingRepository.save(booking);
                log.info("=> [Booking] Đã ghi nhận thanh toán tại quầy bookingId={}, hạn {}",
                        bookingId, booking.getPaymentDueAt());
                sendOfficeReservationEmail(booking, paymentDueAt);
            }, () -> log.error("=> [Booking] Không tìm thấy bookingId={}", bookingId));
        } catch (Exception e) {
            log.error("=> [Booking] Lỗi xử lý office-reserved: {}", e.getMessage(), e);
        }
    }

    private void sendOfficeReservationEmail(Booking booking, LocalDateTime paymentDueAt) {
        try {
            String email = booking.getContactEmail();
            if (email == null || email.isBlank()) {
                log.warn("=> [Booking] Không có contactEmail cho bookingId={}, bỏ qua mail quầy",
                        booking.getId());
                return;
            }

            String tourTitle = "Tour HTravel";
            String startDate = "";
            if (booking.getPriceSnapshot() != null) {
                Object t = booking.getPriceSnapshot().get("tourTitle");
                Object d = booking.getPriceSnapshot().get("startDate");
                if (t != null) tourTitle = t.toString();
                if (d != null) startDate = d.toString();
            }

            String amountStr = booking.getTotalAmount() != null
                    ? String.format("%,.0f VNĐ", booking.getTotalAmount())
                    : "N/A";

            String dueStr = paymentDueAt != null
                    ? paymentDueAt.format(DUE_FMT)
                    : "N/A";

            String name = booking.getContactName() != null && !booking.getContactName().isBlank()
                    ? booking.getContactName() : email;

            Map<String, Object> params = new HashMap<>();
            params.put("name", name);
            params.put("bookingCode", booking.getBookingCode());
            params.put("tourTitle", tourTitle);
            params.put("startDate", startDate);
            params.put("amount", amountStr);
            params.put("paymentDueAt", dueStr);
            params.put("officeAddress", officeAddress);
            params.put("officeHours", officeHours);
            params.put("officeHotline", officeHotline);

            Map<String, Object> event = new HashMap<>();
            event.put("channel", "EMAIL");
            event.put("recipient", email);
            event.put("templateCode", "BOOKING_OFFICE_RESERVATION");
            event.put("param", params);

            kafkaTemplate.send("notification-topic", event);
            log.info("=> [Booking] Đã gửi email hướng dẫn quầy tới {} cho {}",
                    email, booking.getBookingCode());
        } catch (Exception e) {
            log.error("=> [Booking] Lỗi gửi email quầy: {}", e.getMessage(), e);
        }
    }
}
