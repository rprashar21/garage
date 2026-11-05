CREATE TABLE notification_outbox (
    id              UUID PRIMARY KEY,
    enquiry_id      UUID NOT NULL REFERENCES enquiry (id),
    channel         VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count   INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_until    TIMESTAMPTZ,
    claim_token     UUID,
    last_error      VARCHAR(1000),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at         TIMESTAMPTZ,

    CONSTRAINT uq_outbox_enquiry_channel UNIQUE (enquiry_id, channel),
    CONSTRAINT chk_outbox_channel CHECK (channel IN ('EMAIL')),
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED')),
    CONSTRAINT chk_outbox_attempt_count CHECK (attempt_count >= 0)
);

CREATE INDEX idx_outbox_status_next_attempt
    ON notification_outbox (status, next_attempt_at);
