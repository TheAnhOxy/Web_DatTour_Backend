package com.tour.booking.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumer lắng nghe các Dead Letter Topic (DLT).
 *
 * Khi BookingPaymentConsumer xử lý tin nhắn thất bại quá 3 lần,
 * tin nhắn đó sẽ tự động bị đẩy vào Topic "<tên-topic-gốc>.DLT".
 * Class này đứng ở đó để "nhặt" những thư chết và ghi log chi tiết
 * cho kỹ thuật viên vào điều tra sau.
 *
 * Danh sách các DLT được giám sát:
 * - payment-completed-topic.DLT : Lỗi khi cập nhật trạng thái Booking sau khi thu tiền
 * - payment-timeout-topic.DLT   : Lỗi khi hủy Booking do quá hạn thanh toán
 */
@Slf4j
@Service
public class BookingDlqConsumer {

    /**
     * Giám sát DLQ của luồng "Thu tiền thành công".
     * Nếu có thư ở đây, nghĩa là hệ thống đã thu tiền khách xong
     * nhưng KHÔNG cập nhật được trạng thái Booking thành CONFIRMED.
     * => CẦN XỬ LÝ THỦ CÔNG NGAY!
     */
    @KafkaListener(
            topics = "payment-completed-topic.DLT",
            groupId = "booking-dlq-group"
    )
    public void handlePaymentCompletedDlq(ConsumerRecord<String, String> record) {
        log.error("============================================================");
        log.error("[DLQ][CRITICAL] THU TIỀN THÀNH CÔNG NHƯNG KHÔNG CẬP NHẬT BOOKING!");
        log.error("[DLQ] Topic   : {}", record.topic());
        log.error("[DLQ] Key     : {}", record.key());
        log.error("[DLQ] Payload : {}", record.value());
        log.error("[DLQ] Offset  : {}", record.offset());
        log.error("[DLQ] >>> CẦN KIỂM TRA VÀ XỬ LÝ THỦ CÔNG! <<<");
        log.error("============================================================");
        // TODO: Có thể gửi Alert qua Email/Slack cho đội kỹ thuật tại đây
    }

    /**
     * Giám sát DLQ của luồng "Hủy đơn do quá hạn".
     * Nếu có thư ở đây, nghĩa là Booking KHÔNG được hủy
     * dù đã quá hạn thanh toán. Slot trên Redis có thể bị treo.
     */
    @KafkaListener(
            topics = "payment-timeout-topic.DLT",
            groupId = "booking-dlq-group"
    )
    public void handlePaymentTimeoutDlq(ConsumerRecord<String, String> record) {
        log.error("============================================================");
        log.error("[DLQ][WARNING] HẾT HẠN THANH TOÁN NHƯNG KHÔNG HỦY ĐƯỢC BOOKING!");
        log.error("[DLQ] Topic   : {}", record.topic());
        log.error("[DLQ] Key     : {}", record.key());
        log.error("[DLQ] Payload : {}", record.value());
        log.error("[DLQ] Offset  : {}", record.offset());
        log.error("[DLQ] >>> SLOT REDIS CÓ THỂ BỊ TREO. KIỂM TRA NGAY! <<<");
        log.error("============================================================");
    }
}
