INSERT INTO promotions(
    code,
    discount_percent,
    max_discount,
    usage_limit,
    used_count,
    valid_from,
    valid_to,
    is_active
)
VALUES
    ('SUMMER10',10,500000,100,0,NOW(),NOW() + INTERVAL '30 day',true);