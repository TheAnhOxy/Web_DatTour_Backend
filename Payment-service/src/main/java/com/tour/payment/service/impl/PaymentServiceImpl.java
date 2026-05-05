package com.tour.payment.service.impl;

import com.tour.payment.dto.response.PaymentResponse;
import com.tour.payment.entity.Payment;
import com.tour.payment.repository.PaymentRepository;
import com.tour.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ModelMapper modelMapper;

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


}
