CREATE TABLE worker_payment_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    worker_profile_id BIGINT NOT NULL,
    membership_id BIGINT NOT NULL,
    payment_id VARCHAR(255),
    order_id VARCHAR(255) NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    paid_at DATETIME NOT NULL,
    payment_method VARCHAR(100),
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_worker_payment_history_profile FOREIGN KEY (worker_profile_id) REFERENCES worker_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_worker_payment_history_membership FOREIGN KEY (membership_id) REFERENCES worker_memberships(id) ON DELETE CASCADE,
    CONSTRAINT uq_payment_history_order UNIQUE (order_id)
);
