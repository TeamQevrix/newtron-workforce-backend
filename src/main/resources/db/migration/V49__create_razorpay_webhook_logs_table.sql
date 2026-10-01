CREATE TABLE razorpay_webhook_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255),
    order_id VARCHAR(255),
    payment_id VARCHAR(255),
    status VARCHAR(50),
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT uq_razorpay_webhook_event UNIQUE (event_id)
);
