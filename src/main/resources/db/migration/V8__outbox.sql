-- V8: transactional outbox (written in the same transaction as the business change) and consumer de-duplication.
CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    publish_attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500)
);

-- The relay only ever scans unpublished rows.
CREATE INDEX idx_outbox_events_unpublished ON outbox_events(id) WHERE published_at IS NULL;

CREATE TABLE processed_events (
    consumer_group VARCHAR(100) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (consumer_group, event_id)
);
