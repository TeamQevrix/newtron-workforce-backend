ALTER TABLE users 
ADD COLUMN password_reset_token_hash VARCHAR(64) NULL,
ADD COLUMN password_reset_expires_at TIMESTAMP NULL;
