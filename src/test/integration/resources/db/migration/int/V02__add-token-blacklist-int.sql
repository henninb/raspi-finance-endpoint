-- V02: Add token blacklist table for integration tests (H2)
-- Mirrors V23 production migration adapted for H2 syntax.

CREATE TABLE IF NOT EXISTS int.t_token_blacklist (
    token_blacklist_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_hash         VARCHAR(64)              NOT NULL,
    expires_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_token_blacklist_token_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_token_blacklist_expires_at
    ON int.t_token_blacklist (expires_at);
