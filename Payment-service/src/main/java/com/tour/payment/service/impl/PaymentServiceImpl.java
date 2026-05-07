package com.tour.payment.service.impl;

import com.tour.payment.dto.response.PaymentResponse;
import com.tour.payment.entity.Payment;
import com.tour.payment.repository.PaymentRepository;
import com.tour.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ModelMapper modelMapper;

    private final KafkaTemplate<String, Object> kafkaTemplate;

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
    public void processCallback(String txnRef, String status) {
        Payment payment = paymentRepository.findByTransactionId(txnRef)
                .orElseThrow(() -> new RuntimeException("Giao dịch không tồn tại"));

        payment.setStatus(status);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        if ("SUCCESS".equals(status)) {
            // Gửi tin nhắn String thuần túy để tránh lỗi ngoặc kép
            kafkaTemplate.send("payment-completed-topic", txnRef);
            log.info("=> [Kafka] Đã báo SUCCESS cho đơn: {}", txnRef);
        }
    }


}
