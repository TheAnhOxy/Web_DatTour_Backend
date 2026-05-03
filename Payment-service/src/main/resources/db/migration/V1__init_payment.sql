CREATE TABLE IF NOT EXISTS payment_methods (
                                               id BIGSERIAL PRIMARY KEY,
                                               name VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE
    );

CREATE TABLE IF NOT EXISTS payments (
                                        id BIGSERIAL PRIMARY KEY,
                                        booking_id BIGINT NOT NULL,
                                        payment_method_id BIGINT REFERENCES payment_methods(id),
    amount DECIMAL(15,2),
    transaction_id VARCHAR(120) UNIQUE,
    gateway VARCHAR(50),
    payment_url TEXT,
    status VARCHAR(30),
    paid_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );