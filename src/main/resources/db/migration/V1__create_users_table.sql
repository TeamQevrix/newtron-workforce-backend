CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    uuid VARCHAR(36) NOT NULL UNIQUE,

    full_name VARCHAR(100) NOT NULL,

    mobile VARCHAR(15) NOT NULL UNIQUE,

    email VARCHAR(150) UNIQUE,

    password VARCHAR(255),

    role ENUM('WORKER','CLIENT','ADMIN') NOT NULL,

    status ENUM('ACTIVE','INACTIVE','BLOCKED') NOT NULL DEFAULT 'ACTIVE',

    city VARCHAR(100),

    profile_completed BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_mobile ON users(mobile);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);