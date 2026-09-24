CREATE TABLE attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    remark VARCHAR(500) NULL,
    marked_by BIGINT NOT NULL,

    -- BaseEntity audits
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_attendance_work_order FOREIGN KEY (work_order_id) REFERENCES work_orders (id),
    CONSTRAINT fk_attendance_marked_by FOREIGN KEY (marked_by) REFERENCES users (id),
    CONSTRAINT uq_attendance_work_order_date UNIQUE (work_order_id, attendance_date)
);

CREATE INDEX idx_attendance_work_order ON attendance (work_order_id);
CREATE INDEX idx_attendance_date ON attendance (attendance_date);
