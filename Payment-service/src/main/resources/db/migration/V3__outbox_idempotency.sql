CREATE TABLE IF NOT EXISTS outbox_events (
                                               id BIGSERIAL PRIMARY KEY,
                                               aggregate_type VARCHAR(50) NOT NULL,
                                               aggregate_id VARCHAR(120) NOT NULL,
                                               event_type VARCHAR(50) NOT NULL,
                                               payload TEXT NOT NULL,
                                               status VARCHAR(20) NOT NULL,
                                               retry_count INT NOT NULL DEFAULT 0,
                                               sent_at TIMESTAMP,
                                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment_callback_logs (
                                                     id BIGSERIAL PRIMARY KEY,
                                                     idempotency_key VARCHAR(200) NOT NULL,
                                                     gateway VARCHAR(50) NOT NULL,
                                                     transaction_id VARCHAR(120) NOT NULL,
                                                     status VARCHAR(30) NOT NULL,
                                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                     CONSTRAINT uk_payment_callback_idempotency UNIQUE (idempotency_key)
);
