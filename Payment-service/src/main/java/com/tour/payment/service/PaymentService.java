package com.tour.payment.service;

import com.tour.payment.dto.response.PaymentDetailResponse;
import com.tour.payment.dto.response.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse getPaymentByBookingId(Long bookingId);

    PaymentResponse getPaymentByTransactionId(String transactionId);
    void processCallback(String gateway, java.util.Map<String, String> params);
    List<PaymentDetailResponse> getAllPaymentDetails();

    /**
     * Tạo mới hoặc thay thế payment với đúng gateway user chọn.
     * Nếu đã có payment PENDING cùng gateway → trả về luôn.
     * Nếu khác gateway → xóa cũ, tạo mới.
     */
    PaymentResponse initiatePayment(Long bookingId, String gateway, String bookingCode, java.math.BigDecimal amount);

    /**
     * Xác nhận thanh toán Stripe ngay sau redirect (không chờ webhook).
     */
    PaymentResponse confirmStripeSession(String sessionId);

}
