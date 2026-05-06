-- 1. Quyền hạn (Permissions)
INSERT INTO permissions (code) VALUES
                                   ('USER_VIEW'), ('USER_EDIT'), ('TOUR_MANAGE'), ('BOOKING_MANAGE'), ('REPORT_VIEW')
    ON CONFLICT (code) DO NOTHING;

-- 2. Vai trò (Roles)
INSERT INTO roles (name) VALUES
                             ('ADMIN'), ('STAFF'), ('CUSTOMER')
    ON CONFLICT (name) DO NOTHING;

-- 3. Hạng thành viên (Membership Tiers)
INSERT INTO membership_tiers (tier_name, min_points, discount_rate) VALUES
                                                                        ('SILVER', 0, 0.00),
                                                                        ('GOLD', 5000, 5.00),
                                                                        ('PLATINUM', 15000, 10.00)
    ON CONFLICT (tier_name) DO NOTHING;

-- 4. Người dùng mẫu (Users)
-- Pass: password123
INSERT INTO users (email, password_hash, full_name, status, current_points, tier_id, email_verified) VALUES
                                                                                                         ('admin@gotour.com', '$2a$10$8.UnVuG9HHgffUDAlk8q6uy5y.SlZ0G1JvS4G6O/zG7K6mJ5pS6mS', 'Admin System', 'ACTIVE', 20000,
                                                                                                          (SELECT id FROM membership_tiers WHERE tier_name = 'PLATINUM'), true),
                                                                                                         ('khachhang@gmail.com', '$2a$10$8.UnVuG9HHgffUDAlk8q6uy5y.SlZ0G1JvS4G6O/zG7K6mJ5pS6mS', 'Nguyễn Văn Khách', 'ACTIVE', 0,
                                                                                                          (SELECT id FROM membership_tiers WHERE tier_name = 'SILVER'), true)
    ON CONFLICT (email) DO NOTHING;

-- 5. Gán vai trò cho người dùng (Sử dụng Subquery để tránh lỗi ID)
INSERT INTO user_roles (user_id, role_id) VALUES
                                              (
                                                  (SELECT id FROM users WHERE email = 'admin@gotour.com'),
                                                  (SELECT id FROM roles WHERE name = 'ADMIN')
                                              ),
                                              (
                                                  (SELECT id FROM users WHERE email = 'khachhang@gmail.com'),
                                                  (SELECT id FROM roles WHERE name = 'CUSTOMER')
                                              );

-- 6. Gán quyền cho vai trò
-- ADMIN có tất cả các quyền
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'ADMIN'), id FROM permissions;

-- CUSTOMER chỉ có quyền xem (USER_VIEW)
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'CUSTOMER'), id FROM permissions WHERE code = 'USER_VIEW';