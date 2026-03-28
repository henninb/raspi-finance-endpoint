-- V21: Add credit card billing cycle fields to t_account
-- All columns are nullable; only populated when the account is a credit card.
-- Exactly one of {billing_grace_period_days, billing_due_day_same_month,
-- billing_due_day_next_month} should be set per card row.

ALTER TABLE public.t_account
    ADD COLUMN IF NOT EXISTS billing_statement_close_day SMALLINT NULL,
    ADD COLUMN IF NOT EXISTS billing_grace_period_days   SMALLINT NULL,
    ADD COLUMN IF NOT EXISTS billing_due_day_same_month  SMALLINT NULL,
    ADD COLUMN IF NOT EXISTS billing_due_day_next_month  SMALLINT NULL,
    ADD COLUMN IF NOT EXISTS billing_cycle_weekend_shift TEXT     NULL;

ALTER TABLE public.t_account
    ADD CONSTRAINT ck_billing_statement_close_day
        CHECK (billing_statement_close_day BETWEEN 1 AND 31),
    ADD CONSTRAINT ck_billing_grace_period_days
        CHECK (billing_grace_period_days BETWEEN 1 AND 60),
    ADD CONSTRAINT ck_billing_due_day_same_month
        CHECK (billing_due_day_same_month BETWEEN 1 AND 31),
    ADD CONSTRAINT ck_billing_due_day_next_month
        CHECK (billing_due_day_next_month BETWEEN 1 AND 31),
    ADD CONSTRAINT ck_billing_cycle_weekend_shift
        CHECK (billing_cycle_weekend_shift IN ('back', 'forward')),
    ADD CONSTRAINT ck_billing_due_method_exclusive
        CHECK (
            (CASE WHEN billing_grace_period_days  IS NOT NULL THEN 1 ELSE 0 END +
             CASE WHEN billing_due_day_same_month IS NOT NULL THEN 1 ELSE 0 END +
             CASE WHEN billing_due_day_next_month IS NOT NULL THEN 1 ELSE 0 END) <= 1
        );
