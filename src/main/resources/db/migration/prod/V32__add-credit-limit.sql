-- V32: Add credit_limit to t_account for revolving credit accounts
-- (credit, credit_card, business_credit, line_of_credit). NULL means unknown / not applicable.

ALTER TABLE public.t_account
    ADD COLUMN IF NOT EXISTS credit_limit NUMERIC(12, 2) NULL;

ALTER TABLE public.t_account
    ADD CONSTRAINT ck_credit_limit CHECK (credit_limit IS NULL OR credit_limit >= 0);
