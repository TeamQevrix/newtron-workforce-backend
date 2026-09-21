-- PHASE 4B: AGREEMENT DATA MODEL + IMMUTABLE SNAPSHOT

ALTER TABLE agreements
    -- IMMUTABLE SNAPSHOT FIELDS
    ADD COLUMN worker_name_snapshot VARCHAR(255),
    ADD COLUMN company_name_snapshot VARCHAR(255),
    ADD COLUMN job_title_snapshot VARCHAR(255),
    ADD COLUMN job_description_snapshot TEXT,
    ADD COLUMN work_location_snapshot VARCHAR(255),
    ADD COLUMN primary_skill_snapshot VARCHAR(255),

    -- COMMERCIAL SNAPSHOT
    ADD COLUMN commission_payer VARCHAR(50),
    ADD COLUMN commission_amount DECIMAL(12, 2),

    -- CLIENT CUSTOM TERMS
    ADD COLUMN client_custom_terms TEXT,

    -- AGREEMENT VERSION / LOCKING FOUNDATION
    ADD COLUMN agreement_version INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN is_locked BOOLEAN NOT NULL DEFAULT FALSE,

    -- STANDARD TERMS / DOCUMENT / EXPIRY FOUNDATION
    ADD COLUMN standard_terms_version VARCHAR(50),
    ADD COLUMN document_url VARCHAR(1000),
    ADD COLUMN expires_at TIMESTAMP NULL;
