-- Migration: V08__create-token-blacklist.sql
-- Purpose: Persistent JWT token blacklist to survive server restarts
-- Stores SHA-256 hashes of revoked tokens (never raw tokens)

SET client_min_messages TO WARNING;

CREATE TABLE IF NOT EXISTS public.t_token_blacklist (
    token_blacklist_id BIGSERIAL PRIMARY KEY,
    token_hash         VARCHAR(64)              NOT NULL,
    expires_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_token_blacklist_token_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_token_blacklist_expires_at
    ON public.t_token_blacklist (expires_at);
