-- 1. Phương thức thanh toán
INSERT INTO payment_methods (name, is_active) VALUES
                                                  ('VNPAY', true),
                                                  ('MOMO', true),
                                                  ('STRIPE', true),
                                                  ('CASH', true)
    ON CONFLICT DO NOTHING;

-- 2. Giao dịch mẫu (Dựa trên đơn hàng BK-2026-001 và 002)
INSERT INTO payments (booking_id, payment_method_id, amount, transaction_id, gateway, status, paid_at)
VALUES
-- Giao dịch đang chờ thanh toán qua VNPay
((SELECT id FROM payment_methods WHERE name = 'VNPAY'), 1, 5000000.00, NULL, 'VNPAY', 'PENDING', NULL),

-- Giao dịch đã thanh toán thành công qua Momo
((SELECT id FROM payment_methods WHERE name = 'MOMO'), 2, 4500000.00, 'MOMO123456789', 'MOMO', 'SUCCESS', CURRENT_TIMESTAMP)
    ON CONFLICT (transaction_id) DO NOTHING;