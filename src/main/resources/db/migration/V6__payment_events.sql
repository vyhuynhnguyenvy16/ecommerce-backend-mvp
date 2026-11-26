-- V6: audit trail of every payment result VESTRA receives (IPN or status query), valid or not.
CREATE TABLE payment_events (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT REFERENCES payments(id) ON DELETE RESTRICT,
    source VARCHAR(20) NOT NULL,
    provider_order_id VARCHAR(200),
    trans_id BIGINT,
    amount BIGINT,
    result_code INT,
    signature_valid BOOLEAN NOT NULL,
    outcome VARCHAR(40) NOT NULL,
    payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_events_payment_id ON payment_events(payment_id);
CREATE INDEX idx_payment_events_outcome ON payment_events(outcome);
