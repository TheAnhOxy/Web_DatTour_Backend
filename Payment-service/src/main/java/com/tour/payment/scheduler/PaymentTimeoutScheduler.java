package com.tour.payment.scheduler;

import com.tour.payment.entity.Payment;
import com.tour.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentTimeoutProcessor paymentTimeoutProcessor;

    @Value("${payment.timeout.minutes:10}")
    private int timeoutMinutes;

    /**
     * Chạy mỗi 60 giây, tìm các payment PENDING quá hạn và chuyển sang FAILED.
     *
     * KHÔNG dùng @Transactional ở đây — mỗi payment được xử lý trong transaction
     * riêng biệt (REQUIRES_NEW) thông qua PaymentTimeoutProcessor, đảm bảo lỗi
     * của 1 payment không ảnh hưởng đến các payment khác trong cùng batch.
     */
    @Scheduled(fixedDelayString = "${payment.timeout.check-ms:60000}")
    public void expireStalePayments() {
        LocalDateTime onlineCutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);

        // CASH_OFFICE: hủy theo booking.paymentDueAt (booking-service), không theo createdAt payment
        List<Payment> stalePayments = new java.util.ArrayList<>(paymentRepository.findPendingOnlineExpired(onlineCutoff));

        if (stalePayments.isEmpty()) {
            return;
        }

        log.info("[Timeout Scheduler] Tìm thấy {} payment PENDING quá {} phút, tiến hành hủy...",
                stalePayments.size(), timeoutMinutes);

        int successCount = 0;
        int failCount = 0;

        for (Payment payment : stalePayments) {
            try {
                paymentTimeoutProcessor.processTimeout(payment);
                successCount++;
            } catch (Exception ex) {
                failCount++;
                log.error("[Timeout Scheduler] Lỗi khi xử lý timeout cho payment {} (bookingId={}): {}",
                        payment.getTransactionId(), payment.getBookingId(), ex.getMessage());
            }
        }

        log.info("[Timeout Scheduler] Hoàn tất: {} thành công, {} thất bại / tổng {} payment.",
                successCount, failCount, stalePayments.size());
    }
}
