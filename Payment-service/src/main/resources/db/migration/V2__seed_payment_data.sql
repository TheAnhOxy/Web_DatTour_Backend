-- 1. Phương thức thanh toán
INSERT INTO payment_methods (name, is_active) VALUES
                                                  ('STRIPE', true),
                                                  ('SEPAY', true),
                                                  ('CASH_OFFICE', true)
    ON CONFLICT DO NOTHING;

-- 2. Giao dịch mẫu (Dựa trên đơn hàng BK-2026-001 và 002)
INSERT INTO payments (booking_id, payment_method_id, amount, transaction_id, gateway, status, paid_at)
VALUES
-- Giao dịch đang chờ thanh toán qua Stripe
((SELECT id FROM payment_methods WHERE name = 'STRIPE'), 1, 5000000.00, NULL, 'STRIPE', 'PENDING', NULL),

-- Giao dịch đã thanh toán thành công qua SePay
((SELECT id FROM payment_methods WHERE name = 'SEPAY'), 2, 4500000.00, 'SEPAY123456789', 'SEPAY', 'SUCCESS', CURRENT_TIMESTAMP)
    ON CONFLICT (transaction_id) DO NOTHING;