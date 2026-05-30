package com.tour.payment.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumer lắng nghe các Dead Letter Topic (DLT) của Payment-service.
 *
 * Khi PaymentConsumer xử lý tin nhắn từ Booking-service thất bại quá 3 lần,
 * tin nhắn đó sẽ tự động bị đẩy vào Topic "booking-created-topic.DLT".
 *
 * Hậu quả nếu có thư ở DLT này:
 * - Đơn hàng đã được Booking-service tạo (trạng thái PENDING)
 * - NHƯNG Payment-service KHÔNG tạo được hóa đơn thanh toán
 * - Khách hàng sẽ không thể thanh toán được dù đã có đơn hàng!
 */
@Slf4j
@Service
public class PaymentDlqConsumer {

    /**
     * Giám sát DLQ của luồng "Tạo hóa đơn thanh toán".
     * Nếu có thư ở đây, nghĩa là Booking đã được tạo NHƯNG
     * Payment-service không tạo được Payment Intent.
     * Khách hàng sẽ bị mắc kẹt, không có link thanh toán!
     */
    @KafkaListener(
            topics = "booking-created-topic.DLT",
            groupId = "payment-dlq-group"
    )
    public void handleBookingCreatedDlq(ConsumerRecord<String, String> record) {
        log.error("============================================================");
        log.error("[DLQ][CRITICAL] BOOKING TẠO THÀNH CÔNG NHƯNG KHÔNG TẠO ĐƯỢC PAYMENT INTENT!");
        log.error("[DLQ] Topic   : {}", record.topic());
        log.error("[DLQ] Key     : {}", record.key());
        log.error("[DLQ] Payload : {}", record.value());
        log.error("[DLQ] Offset  : {}", record.offset());
        log.error("[DLQ] >>> KHÁCH HÀNG KHÔNG THỂ THANH TOÁN. XỬ LÝ THỦ CÔNG NGAY! <<<");
        log.error("============================================================");
        // TODO: Có thể gửi Alert qua Email/Slack cho đội kỹ thuật tại đây
    }
}
