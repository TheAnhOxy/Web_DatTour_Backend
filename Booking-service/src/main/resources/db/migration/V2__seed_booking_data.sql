-- 1. SEED BOOKINGS
INSERT INTO bookings (booking_code, user_id, departure_id, total_amount, paid_amount, status, price_snapshot, promotion_snapshot)
VALUES
    ('BK-2026-001', 1, 1, 5000000.00, 0, 'PENDING',
     '{"tour_title": "Tour Phú Quốc 3N2D", "adult_price": 5000000}', NULL),
    ('BK-2026-002', 2, 1, 4500000.00, 4500000.00, 'CONFIRMED',
     '{"tour_title": "Tour Phú Quốc 3N2D", "adult_price": 5000000}',
     '{"code": "HE2026", "discount_amount": 500000}'),
    ('BK-2026-003', 2, 2, 10000000.00, 0, 'CANCELLED',
     '{"tour_title": "Tour Châu Âu 7N6D", "adult_price": 10000000}', NULL)
    ON CONFLICT (booking_code) DO NOTHING;

-- 2. SEED PASSENGERS (Sử dụng subquery để lấy ID theo mã booking)
-- Xóa sạch hành khách của 3 đơn này để chèn lại cho đúng
DELETE FROM passengers WHERE booking_id IN (
    SELECT id FROM bookings WHERE booking_code IN ('BK-2026-001', 'BK-2026-002', 'BK-2026-003')
);

INSERT INTO passengers (booking_id, full_name, dob, gender, age_group, id_card_number)
VALUES
    ((SELECT id FROM bookings WHERE booking_code = 'BK-2026-001'), 'Nguyễn Thế Anh', '2004-01-01', 'MALE', 'ADULT', '038094001234'),
    ((SELECT id FROM bookings WHERE booking_code = 'BK-2026-001'), 'Trần Thị Trang', '2004-05-20', 'FEMALE', 'ADULT', '038094005678'),
    ((SELECT id FROM bookings WHERE booking_code = 'BK-2026-002'), 'Nguyễn Văn Khách', '1990-10-10', 'MALE', 'ADULT', '038090001111'),
    ((SELECT id FROM bookings WHERE booking_code = 'BK-2026-003'), 'Lê Văn Hủy', '1985-02-02', 'MALE', 'ADULT', '038085002222');

-- 3. SEED BOOKING_NOTES
DELETE FROM booking_notes WHERE booking_id IN (
    SELECT id FROM bookings WHERE booking_code IN ('BK-2026-001', 'BK-2026-002', 'BK-2026-003')
);

INSERT INTO booking_notes (booking_id, content)
VALUES
    ((SELECT id FROM bookings WHERE booking_code = 'BK-2026-001'), 'Vui lòng sắp xếp phòng hướng biển.'),
    ((SELECT id FROM bookings WHERE booking_code = 'BK-2026-002'), 'Dị ứng hải sản.'),
    ((SELECT id FROM bookings WHERE booking_code = 'BK-2026-003'), 'Khách yêu cầu hủy.');

-- 4. SEED CANCELLATIONS
DELETE FROM cancellations WHERE booking_id = (SELECT id FROM bookings WHERE booking_code = 'BK-2026-003');

INSERT INTO cancellations (booking_id, reason, refund_amount, cancelled_at)
VALUES
    ((SELECT id FROM bookings WHERE booking_code = 'BK-2026-003'), 'Lý do cá nhân.', 0.00, CURRENT_TIMESTAMP)
    ON CONFLICT (booking_id) DO NOTHING;