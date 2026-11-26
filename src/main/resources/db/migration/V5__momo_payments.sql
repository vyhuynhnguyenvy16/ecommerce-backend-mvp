-- V5: MoMo payments. Additive only: existing orders keep COD behaviour via column defaults.

ALTER TABLE orders
    ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'COD',
    ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED';

-- The V1 payments table (one row per order) was never used by the application; it now holds the
-- order-level payment record. Individual MoMo requests are stored in payment_attempts.
ALTER TABLE payments
    ADD COLUMN method VARCHAR(20) NOT NULL DEFAULT 'MOMO',
    ADD COLUMN provider_trans_id BIGINT UNIQUE,
    ADD COLUMN expires_at TIMESTAMP,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX idx_payments_status_expires_at ON payments(status, expires_at);

CREATE TABLE payment_attempts (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE RESTRICT,
    provider_order_id VARCHAR(64) NOT NULL UNIQUE,
    request_id VARCHAR(64) NOT NULL,
    amount BIGINT NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL,
    pay_url TEXT,
    result_code INT,
    trans_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_attempts_payment_id ON payment_attempts(payment_id);
