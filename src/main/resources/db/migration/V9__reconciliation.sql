-- V9: reconciliation runs and the discrepancies they find. An item is raised once per (type, reference).
CREATE TABLE reconciliation_runs (
    id BIGSERIAL PRIMARY KEY,
    trigger VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    queried_attempts INT NOT NULL DEFAULT 0,
    new_items INT NOT NULL DEFAULT 0
);

CREATE TABLE reconciliation_items (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES reconciliation_runs(id) ON DELETE RESTRICT,
    type VARCHAR(40) NOT NULL,
    reference VARCHAR(100) NOT NULL,
    payment_id BIGINT REFERENCES payments(id) ON DELETE RESTRICT,
    order_id BIGINT REFERENCES orders(id) ON DELETE RESTRICT,
    details VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED')),
    resolution_note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    CONSTRAINT uq_reconciliation_items_type_reference UNIQUE (type, reference)
);

CREATE INDEX idx_reconciliation_items_status ON reconciliation_items(status);
