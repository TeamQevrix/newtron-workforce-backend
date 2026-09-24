-- Create settlements table Flyway Migration
CREATE TABLE settlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_id BIGINT NOT NULL,
    settlement_month INT NOT NULL,
    settlement_year INT NOT NULL,
    worker_payable_amount DECIMAL(12,2),
    newtron_commission_amount DECIMAL(12,2),
    total_client_payable_amount DECIMAL(12,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_settlements_work_order FOREIGN KEY (work_order_id) REFERENCES work_orders(id),
    CONSTRAINT uq_settlements_work_order_month_year UNIQUE (work_order_id, settlement_month, settlement_year)
);

CREATE INDEX idx_settlements_work_order ON settlements (work_order_id, deleted);
