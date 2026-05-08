package com.tour.payment.consumer;

import com.tour.payment.entity.Payment;
import com.tour.payment.entity.PaymentMethod;
import com.tour.payment.repository.PaymentMethodRepository;
import com.tour.payment.repository.PaymentRepository;
import com.tour.payment.service.gateway.PaymentGatewayStrategy;
import com.tour.payment.service.gateway.PaymentGatewayStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentConsumer {
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository methodRepository;
    private final PaymentGatewayStrategyFactory gatewayStrategyFactory;

    @KafkaListener(topics = "booking-created-topic", groupId = "payment-group")
    public void handleBookingCreated(Map<String, Object> message) {
        String bookingCode = message.get("bookingCode").toString();

        //  Kiểm tra phương thức thanh toán (VD: mặc định lấy payment method id=1)
        PaymentMethod method = methodRepository.findById(1L)
                .filter(PaymentMethod::getIsActive)
                .orElseThrow(() -> new RuntimeException("Phương thức thanh toán chưa kích hoạt!"));

        if (paymentRepository.findByTransactionId(bookingCode).isEmpty()) {
            String gateway = method.getName() == null ? "CASH_OFFICE" : method.getName();
            PaymentGatewayStrategy strategy = gatewayStrategyFactory.getStrategy(gateway);
            Payment payment = strategy.createPayment(message, method);

            paymentRepository.save(payment);
            log.info("=> Đã tạo Payment Intent có URL cho đơn: {}", bookingCode);
        }
    }
}