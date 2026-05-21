-- Bổ sung CASH_OFFICE nếu DB cũ chỉ có STRIPE/SEPAY
INSERT INTO payment_methods (name, is_active)
SELECT 'CASH_OFFICE', true
WHERE NOT EXISTS (
    SELECT 1 FROM payment_methods WHERE UPPER(name) = 'CASH_OFFICE'
);
