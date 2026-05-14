package com.tour.payment.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour.payment.entity.Payment;
import com.tour.payment.outbox.OutboxEvent;
import com.tour.payment.outbox.OutboxEventRepository;
import com.tour.payment.outbox.OutboxStatus;
import com.tour.payment.repository.PaymentRepository;
import com.tour.payment.service.state.PaymentEvent;
import com.tour.payment.service.state.PaymentStateMachine;
import com.tour.payment.service.state.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine paymentStateMachine;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${payment.timeout.minutes:10}")
    private int timeoutMinutes;

    /**
     * Chạy mỗi 60 giây, tìm các payment PENDING quá hạn và chuyển sang FAILED.
     * Sau đó ghi outbox event để Booking-service hủy booking tương ứng.
     */
    @Scheduled(fixedDelayString = "${payment.timeout.check-ms:60000}")
    @Transactional
    public void expireStalePayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Payment> stalePayments = paymentRepository.findPendingExpired(cutoff);

        if (stalePayments.isEmpty()) {
            return;
        }

        log.info("[Timeout Scheduler] Tìm thấy {} payment PENDING quá {} phút, tiến hành hủy...",
                stalePayments.size(), timeoutMinutes);

        for (Payment payment : stalePayments) {
            try {
                PaymentStatus nextStatus = paymentStateMachine.transition(PaymentStatus.PENDING, PaymentEvent.TIMEOUT);
                payment.setStatus(nextStatus.name());
                paymentRepository.save(payment);

                outboxEventRepository.save(buildTimeoutEvent(payment));

                log.info("[Timeout Scheduler] Payment {} (bookingId={}) đã bị TIMEOUT → {}",
                        payment.getTransactionId(), payment.getBookingId(), nextStatus);
            } catch (Exception ex) {
                log.error("[Timeout Scheduler] Lỗi khi xử lý timeout cho payment {}: {}",
                        payment.getTransactionId(), ex.getMessage());
            }
        }
    }

    private OutboxEvent buildTimeoutEvent(Payment payment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", payment.getTransactionId());
        payload.put("bookingId", payment.getBookingId());
        payload.put("status", payment.getStatus());

        return OutboxEvent.builder()
                .aggregateType("PAYMENT")
                .aggregateId(payment.getTransactionId())
                .eventType("payment-timeout-topic")
                .payload(toJson(payload))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Cannot serialize timeout payload", ex);
        }
    }
}
