package com.tour.booking.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour.booking.entity.Booking;
import com.tour.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingPaymentConsumer {

    private final BookingRepository bookingRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Payment-service gửi payload JSON:
     * {"transactionId":"SEVQRBK123...","bookingId":56,"amount":10000,"gateway":"SEPAY","status":"SUCCESS"}
     */
    @KafkaListener(topics = "payment-completed-topic", groupId = "booking-group")
    public void handlePaymentSuccess(String message) {
        log.info("=> [Booking] Nhận payment-completed: {}", message);
        try {
            Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<>() {});

            Object bookingIdRaw = payload.get("bookingId");
            if (bookingIdRaw != null) {
                Long bookingId = Long.valueOf(bookingIdRaw.toString());
                bookingRepository.findById(bookingId).ifPresentOrElse(booking -> {
                    booking.setStatus("CONFIRMED");
                    bookingRepository.save(booking);
                    log.info("=> [Booking] CONFIRMED bookingId={}", bookingId);
                    sendBookingConfirmedEmail(booking, payload);
                }, () -> log.error("=> [Booking] Không tìm thấy bookingId={}", bookingId));
                return;
            }

            Object txIdRaw = payload.get("transactionId");
            if (txIdRaw != null) {
                String txId = txIdRaw.toString();
                String bookingCode = txId.startsWith("SEVQR") ? txId.substring(5) : txId;
                bookingRepository.findByBookingCode(bookingCode).ifPresentOrElse(booking -> {
                    booking.setStatus("CONFIRMED");
                    bookingRepository.save(booking);
                    log.info("=> [Booking] CONFIRMED bookingCode={}", bookingCode);
                    sendBookingConfirmedEmail(booking, payload);
                }, () -> log.error("=> [Booking] Không tìm thấy bookingCode={}", bookingCode));
                return;
            }

            log.error("=> [Booking] Payload không có bookingId lẫn transactionId: {}", message);

        } catch (Exception e) {
            String bookingCode = message.replace("\"", "").trim();
            log.warn("=> [Booking] Parse JSON thất bại, thử plain string: {}", bookingCode);
            bookingRepository.findByBookingCode(bookingCode).ifPresentOrElse(booking -> {
                booking.setStatus("CONFIRMED");
                bookingRepository.save(booking);
                log.info("=> [Booking] CONFIRMED (fallback) bookingCode={}", bookingCode);
                sendBookingConfirmedEmail(booking, Map.of());
            }, () -> log.error("=> [Booking] Không tìm thấy đơn: {}", bookingCode));
        }
    }

    @KafkaListener(topics = "payment-timeout-topic", groupId = "booking-group")
    public void handlePaymentTimeout(String message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<>() {});
            Object bookingIdRaw = payload.get("bookingId");
            if (bookingIdRaw == null) {
                log.error("=> [Booking] Timeout event thiếu bookingId: {}", message);
                return;
            }

            Long bookingId = Long.valueOf(bookingIdRaw.toString());
            log.info("=> [Booking] Timeout cho bookingId={}", bookingId);

            bookingRepository.findById(bookingId).ifPresentOrElse(booking -> {
                if ("PENDING".equals(booking.getStatus())) {
                    booking.setStatus("CANCELLED");
                    bookingRepository.save(booking);
                    log.info("=> [Booking] HỦY booking {} do hết thời gian", bookingId);
                } else {
                    log.info("=> [Booking] Bỏ qua timeout, booking {} đang ở trạng thái: {}",
                            bookingId, booking.getStatus());
                }
            }, () -> log.error("=> [Booking] Không tìm thấy bookingId={}", bookingId));

        } catch (Exception e) {
            log.error("=> [Booking] Lỗi xử lý timeout: {}", e.getMessage());
        }
    }

    private void sendBookingConfirmedEmail(Booking booking, Map<String, Object> paymentPayload) {
        try {
            String email = booking.getContactEmail();
            if (email == null || email.isBlank()) {
                log.warn("=> [Booking] Không có contactEmail cho bookingId={}, bỏ qua gửi mail", booking.getId());
                return;
            }

            String tourTitle = "N/A";
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

            String gateway = paymentPayload.getOrDefault("gateway", "N/A").toString();

            Map<String, Object> params = new HashMap<>();
            params.put("name", booking.getContactName() != null ? booking.getContactName() : email);
            params.put("bookingCode", booking.getBookingCode());
            params.put("tourTitle", tourTitle);
            params.put("startDate", startDate);
            params.put("amount", amountStr);
            params.put("gateway", gateway);

            Map<String, Object> event = new HashMap<>();
            event.put("channel", "EMAIL");
            event.put("recipient", email);
            event.put("templateCode", "BOOKING_CONFIRMED");
            event.put("param", params);

            kafkaTemplate.send("notification-topic", event);
            log.info("=> [Booking] Đã gửi notification email tới {} cho booking {}", email, booking.getBookingCode());
        } catch (Exception e) {
            log.error("=> [Booking] Lỗi gửi notification email: {}", e.getMessage());
        }
    }
}
