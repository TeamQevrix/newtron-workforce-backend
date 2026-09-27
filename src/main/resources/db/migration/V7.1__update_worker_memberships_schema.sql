ALTER TABLE worker_memberships
    ADD COLUMN plan VARCHAR(255) NOT NULL,
    ADD COLUMN currency VARCHAR(255) NOT NULL,
    ADD COLUMN activated_at DATETIME(6) NULL,
    ADD COLUMN expires_at DATETIME(6) NULL,
    ADD COLUMN payment_provider VARCHAR(255) NULL,
    ADD COLUMN payment_order_id VARCHAR(255) NOT NULL,
    ADD COLUMN payment_payment_id VARCHAR(255) NULL,
    ADD COLUMN payment_signature VARCHAR(255) NULL;

ALTER TABLE worker_memberships
    ADD CONSTRAINT UK_worker_memberships_payment_order_id
    UNIQUE (payment_order_id);
