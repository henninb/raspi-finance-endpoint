-- V31: Add billing_statement_close_business_day to t_account
-- For issuers (e.g. US Bank) whose statements close on the Nth business day of the
-- month (weekdays, excluding US federal holidays) rather than a fixed calendar day.
-- Mutually exclusive with billing_statement_close_day.

ALTER TABLE public.t_account
    ADD COLUMN IF NOT EXISTS billing_statement_close_business_day SMALLINT NULL;

ALTER TABLE public.t_account
    ADD CONSTRAINT ck_billing_statement_close_business_day
        CHECK (billing_statement_close_business_day BETWEEN 1 AND 23),
    ADD CONSTRAINT ck_billing_close_method_exclusive
        CHECK (billing_statement_close_day IS NULL OR billing_statement_close_business_day IS NULL);
