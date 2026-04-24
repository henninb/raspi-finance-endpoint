-- Migration: V23__create-token-blacklist.sql
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

COMMENT ON TABLE  public.t_token_blacklist                IS 'Revoked JWT tokens (SHA-256 hash); enables logout revocation across restarts';
COMMENT ON COLUMN public.t_token_blacklist.token_hash     IS 'SHA-256 hex digest of the raw JWT — raw tokens are never stored';
COMMENT ON COLUMN public.t_token_blacklist.expires_at     IS 'Original token expiry; rows past this time can be pruned';
