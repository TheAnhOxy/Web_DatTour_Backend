INSERT INTO notifications (user_id, template_code, title, content, channel, status, sent_at)
VALUES
    (1, 'WELCOME', 'Chào mừng bạn đến với GoTour', 'Cảm ơn bạn Thế Anh đã đăng ký thành viên.', 'EMAIL', 'SENT', CURRENT_TIMESTAMP),
    (2, 'BOOKING_SUCCESS', 'Đặt tour thành công', 'Đơn hàng BK-2026-002 của bạn đã được xác nhận.', 'EMAIL', 'SENT', CURRENT_TIMESTAMP)
    ON CONFLICT DO NOTHING; -- Đảm bảo dấu chấm phẩy nằm ở cuối cùng