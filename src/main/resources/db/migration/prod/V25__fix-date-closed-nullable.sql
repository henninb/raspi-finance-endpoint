-- V25: Fix date_closed column — change from NOT NULL/epoch-default to nullable/null-default.
-- The epoch value (TO_TIMESTAMP(0)) was used as a sentinel for "never closed".
-- NULL is the correct semantic: NULL means the account has never been closed.
-- Rows still carrying the epoch value are converted to NULL in the data migration below.

ALTER TABLE public.t_account
    ALTER COLUMN date_closed DROP NOT NULL,
    ALTER COLUMN date_closed SET DEFAULT NULL;

UPDATE public.t_account
SET date_closed = NULL
WHERE date_closed = TO_TIMESTAMP(0);
