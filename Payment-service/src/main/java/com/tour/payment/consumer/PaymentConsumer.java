package com.tour.payment.consumer;

import com.tour.payment.entity.Payment;
import com.tour.payment.entity.PaymentMethod;
import com.tour.payment.repository.PaymentMethodRepository;
import com.tour.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentConsumer {
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository methodRepository;

    @KafkaListener(topics = "booking-created-topic", groupId = "payment-group")
    public void handleBookingCreated(Map<String, Object> message) {
        String bookingCode = message.get("bookingCode").toString();

        // 1. Kiểm tra phương thức thanh toán (VD: mặc định lấy VNPAY id=1)
        PaymentMethod method = methodRepository.findById(1L)
                .filter(PaymentMethod::getIsActive)
                .orElseThrow(() -> new RuntimeException("Phương thức thanh toán chưa kích hoạt!"));

        if (paymentRepository.findByTransactionId(bookingCode).isEmpty()) {
            // 2. Giả lập tạo URL thanh toán
            String fakeUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?order=" + bookingCode;

            Payment payment = Payment.builder()
                    .bookingId(Long.valueOf(message.get("bookingId").toString()))
                    .transactionId(bookingCode)
                    .amount(new BigDecimal(message.get("totalAmount").toString()))
                    .status("PENDING")
                    .gateway("VNPAY")
                    .paymentMethod(method)
                    .paymentUrl(fakeUrl) // Gán URL vào đây
                    .build();

            paymentRepository.save(payment);
            log.info("=> Đã tạo Payment Intent có URL cho đơn: {}", bookingCode);
        }
    }
}