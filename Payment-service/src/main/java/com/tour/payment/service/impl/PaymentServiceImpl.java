package com.tour.payment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour.payment.dto.response.PaymentDetailResponse;
import com.tour.payment.dto.response.PaymentResponse;
import com.tour.payment.entity.Payment;
import com.tour.payment.entity.PaymentMethod;
import com.tour.payment.idempotency.PaymentCallbackLog;
import com.tour.payment.idempotency.PaymentCallbackLogRepository;
import com.tour.payment.outbox.OutboxEvent;
import com.tour.payment.outbox.OutboxEventRepository;
import com.tour.payment.outbox.OutboxStatus;
import com.tour.payment.repository.PaymentMethodRepository;
import com.tour.payment.repository.PaymentRepository;
import com.tour.payment.service.PaymentService;
import com.tour.payment.service.callback.PaymentCallbackData;
import com.tour.payment.service.gateway.PaymentGatewayStrategy;
import com.tour.payment.service.gateway.PaymentGatewayStrategyFactory;
import com.tour.payment.service.stripe.StripeService;
import com.tour.payment.service.state.PaymentEvent;
import com.tour.payment.service.state.PaymentStateMachine;
import com.tour.payment.service.state.PaymentStatus;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository methodRepository;
    private final ModelMapper modelMapper;
    private final PaymentStateMachine paymentStateMachine;
    private final PaymentGatewayStrategyFactory gatewayStrategyFactory;
    private final PaymentCallbackLogRepository callbackLogRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final StripeService stripeService;

    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch cho đơn hàng này!"));

        return modelMapper.map(payment, PaymentResponse.class);
    }

    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Mã giao dịch không tồn tại!"));

        return modelMapper.map(payment, PaymentResponse.class);
    }

    @Override
    @Transactional
    public PaymentResponse initiatePayment(Long bookingId, String gateway, String bookingCodeHint, BigDecimal amountHint) {
        String gatewayUpper = gateway.toUpperCase();

        Optional<Payment> currentOpt = paymentRepository.findByBookingId(bookingId);
        String bookingCode = bookingCodeHint;
        BigDecimal amount = amountHint;
        if (currentOpt.isPresent()) {
            Payment current = currentOpt.get();
            if (bookingCode == null || bookingCode.isBlank()) {
                bookingCode = current.getBookingCode();
            }
            if (amount == null) {
                amount = current.getAmount();
            }

            boolean sameGateway = gatewayUpper.equals(current.getGateway());
            boolean isPending = "PENDING".equals(current.getStatus());

            if (sameGateway && isPending) {
                log.info("[Initiate] Dùng lại payment gateway={} cho bookingId={}", gatewayUpper, bookingId);
                return modelMapper.map(current, PaymentResponse.class);
            }

            if (isPending) {
                paymentRepository.delete(current);
                paymentRepository.flush();
                log.info("[Initiate] Đã xóa payment cũ gateway={} cho bookingId={}", current.getGateway(), bookingId);
            }
        }

        // Tạo payment mới với đúng gateway
        PaymentMethod method = methodRepository.findByNameIgnoreCase(gatewayUpper)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment method: " + gatewayUpper));

        if (bookingCode == null || bookingCode.isBlank() || amount == null) {
            throw new RuntimeException("Không tìm thấy thông tin booking để tạo payment mới");
        }

        Map<String, Object> message = new java.util.HashMap<>();
        message.put("bookingId", bookingId);
        message.put("bookingCode", bookingCode);
        message.put("totalAmount", amount);

        PaymentGatewayStrategy strategy = gatewayStrategyFactory.getStrategy(gatewayUpper);
        Payment newPayment = strategy.createPayment(message, method);
        newPayment.setBookingCode(bookingCode);
        paymentRepository.save(newPayment);

        log.info("[Initiate] Tạo payment mới gateway={} cho bookingId={}", gatewayUpper, bookingId);
        return modelMapper.map(newPayment, PaymentResponse.class);
    }

    @Override
    @Transactional
    public PaymentResponse confirmStripeSession(String sessionId) {
        Payment payment = paymentRepository.findByTransactionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch Stripe: " + sessionId));

        if (PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
            return modelMapper.map(payment, PaymentResponse.class);
        }

        Session session = stripeService.retrieveSession(sessionId);
        String stripeStatus = session.getPaymentStatus();

        if ("paid".equals(stripeStatus)) {
            Map<String, String> params = new HashMap<>();
            params.put("transactionId", sessionId);
            params.put("status", "SUCCESS");
            params.put("idempotencyKey", "STRIPE:" + sessionId + ":confirm");
            processCallback("STRIPE", params);
        } else if ("unpaid".equals(stripeStatus)) {
            log.info("[Stripe Confirm] Session {} chưa thanh toán", sessionId);
        }

        return getPaymentByTransactionId(sessionId);
    }

    @Override
    @Transactional
    public void processCallback(String gateway, Map<String, String> params) {
        PaymentGatewayStrategy strategy = gatewayStrategyFactory.getStrategy(gateway);
        PaymentCallbackData callbackData = strategy.parseCallback(params);
        String idempotencyKey = callbackData.getIdempotencyKey();

        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            idempotencyKey = gateway + ":" + callbackData.getTransactionId() + ":" + callbackData.getStatus();
        }

        try {
            callbackLogRepository.save(PaymentCallbackLog.builder()
                    .idempotencyKey(idempotencyKey)
                    .gateway(gateway)
                    .transactionId(callbackData.getTransactionId())
                    .status(callbackData.getStatus())
                    .build());
        } catch (DataIntegrityViolationException ex) {
            log.info("Callback ignored due to idempotency key: {}", idempotencyKey);
            return;
        }

        Payment payment = paymentRepository.findByTransactionId(callbackData.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Giao dịch không tồn tại"));

        PaymentStatus currentStatus = resolveStatus(payment.getStatus());
        if (currentStatus == PaymentStatus.SUCCESS && "SUCCESS".equalsIgnoreCase(callbackData.getStatus())) {
            log.info("Payment {} đã SUCCESS, bỏ qua callback trùng", callbackData.getTransactionId());
            return;
        }
        if ((currentStatus == PaymentStatus.FAILED || currentStatus == PaymentStatus.CANCELED)
                && "FAILED".equalsIgnoreCase(callbackData.getStatus())) {
            log.info("Payment {} đã ở trạng thái kết thúc, bỏ qua callback trùng", callbackData.getTransactionId());
            return;
        }
        PaymentEvent event = resolveEvent(callbackData.getStatus());
        PaymentStatus nextStatus = paymentStateMachine.transition(currentStatus, event);
        payment.setStatus(nextStatus.name());
        if (nextStatus == PaymentStatus.SUCCESS) {
            payment.setPaidAt(LocalDateTime.now());
        }
        paymentRepository.save(payment);

        if (nextStatus == PaymentStatus.SUCCESS) {
            outboxEventRepository.save(buildPaymentCompletedEvent(payment));
        }
    }
    @Override
    public List<PaymentDetailResponse> getAllPaymentDetails() {
        List<Payment> payments = paymentRepository.findAll();

        return payments.stream().map(p -> PaymentDetailResponse.builder()
                .transactionId(p.getTransactionId())
                .amount(p.getAmount())
                .paymentStatus(p.getStatus())
                .paidAt(p.getPaidAt())
                .paymentUrl(p.getPaymentUrl())
                .paymentMethodName(p.getPaymentMethod() != null ? p.getPaymentMethod().getName() : "N/A")
                // QUAN TRỌNG: Phải map bookingId từ Entity sang DTO
                .bookingId(p.getBookingId())
                .build()).toList();
    }

    private OutboxEvent buildPaymentCompletedEvent(Payment payment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", payment.getTransactionId());
        payload.put("bookingId", payment.getBookingId());
        payload.put("amount", payment.getAmount());
        payload.put("gateway", payment.getGateway());
        payload.put("status", payment.getStatus());

        return OutboxEvent.builder()
                .aggregateType("PAYMENT")
                .aggregateId(payment.getTransactionId())
                .eventType("payment-completed-topic")
                .payload(toJson(payload))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Cannot serialize outbox payload", ex);
        }
    }

    private PaymentStatus resolveStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return PaymentStatus.PENDING;
        }
        try {
            return PaymentStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + rawStatus);
        }
    }

    private PaymentEvent resolveEvent(String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            throw new RuntimeException("Thiếu trạng thái callback");
        }

        String normalized = rawStatus.trim().toUpperCase();
        return switch (normalized) {
            case "SUCCESS" -> PaymentEvent.CALLBACK_SUCCESS;
            case "FAILED" -> PaymentEvent.CALLBACK_FAILED;
            case "CANCELED" -> PaymentEvent.CANCEL;
            case "TIMEOUT" -> PaymentEvent.TIMEOUT;
            default -> throw new RuntimeException("Trạng thái callback không hợp lệ: " + rawStatus);
        };
    }


}
