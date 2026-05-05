-- Đảm bảo tên file là V1__init_notification.sql (CÓ 2 DẤU GẠCH DƯỚI)
CREATE TABLE IF NOT EXISTS notifications (
                                             id BIGSERIAL PRIMARY KEY,
                                             user_id BIGINT NOT NULL,
                                             template_code VARCHAR(100),
    title VARCHAR(255),
    content TEXT,
    channel VARCHAR(30),
    status VARCHAR(30) DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );