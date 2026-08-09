-- SPRINT B2 - STEP 3: Worker Address Details Migration
ALTER TABLE worker_addresses
    ADD COLUMN state_id BIGINT NOT NULL,
    ADD COLUMN district_id BIGINT NOT NULL,
    ADD COLUMN city_id BIGINT NOT NULL,
    ADD COLUMN pincode VARCHAR(6) NOT NULL,
    ADD COLUMN current_address VARCHAR(255) NOT NULL,
    ADD COLUMN landmark VARCHAR(255) NULL,
    ADD COLUMN latitude DOUBLE NULL,
    ADD COLUMN longitude DOUBLE NULL;
