CREATE TABLE IF NOT EXISTS bookings (
                                        id BIGSERIAL PRIMARY KEY,
                                        booking_code VARCHAR(30) UNIQUE,
    user_id BIGINT NOT NULL,
    departure_id BIGINT NOT NULL,
    total_amount DECIMAL(15,2),
    paid_amount DECIMAL(15,2) DEFAULT 0,
    status VARCHAR(30),
    price_snapshot JSONB,
    promotion_snapshot JSONB,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS passengers (
                                          id BIGSERIAL PRIMARY KEY,
                                          booking_id BIGINT REFERENCES bookings(id) ON DELETE CASCADE,
    full_name VARCHAR(120),
    dob DATE,
    gender VARCHAR(20),
    age_group VARCHAR(20),
    id_card_number VARCHAR(30),
    passport_number VARCHAR(50)
    );

CREATE TABLE IF NOT EXISTS booking_notes (
                                             id BIGSERIAL PRIMARY KEY,
                                             booking_id BIGINT REFERENCES bookings(id) ON DELETE CASCADE,
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS cancellations (
                                             id BIGSERIAL PRIMARY KEY,
                                             booking_id BIGINT UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    reason TEXT,
    refund_amount DECIMAL(15,2),
    cancelled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );