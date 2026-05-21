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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Xử lý timeout cho từng payment trong transaction riêng biệt (REQUIRES_NEW).
 * Tách ra khỏi PaymentTimeoutScheduler để Spring AOP có thể intercept và tạo
 * transaction mới cho mỗi payment — tránh toàn bộ batch bị rollback khi 1 payment lỗi.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutProcessor {

    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine paymentStateMachine;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Chuyển trạng thái payment sang FAILED (TIMEOUT) và ghi outbox event.
     * Mỗi lần gọi chạy trong transaction mới, hoàn toàn độc lập với các payment khác.
     *
     * @return true nếu xử lý thành công, false nếu có lỗi
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processTimeout(Payment payment) {
        if (payment.getTransactionId() == null || payment.getTransactionId().isBlank()) {
            log.warn("[Timeout Processor] Bỏ qua payment id={} (bookingId={}) vì transactionId null — record bị lỗi dữ liệu.",
                    payment.getId(), payment.getBookingId());
            return false;
        }

        PaymentStatus nextStatus = paymentStateMachine.transition(PaymentStatus.PENDING, PaymentEvent.TIMEOUT);
        payment.setStatus(nextStatus.name());
        paymentRepository.save(payment);

        outboxEventRepository.save(buildTimeoutEvent(payment));

        log.info("[Timeout Scheduler] Payment {} (bookingId={}) đã bị TIMEOUT → {}",
                payment.getTransactionId(), payment.getBookingId(), nextStatus);
        return true;
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
