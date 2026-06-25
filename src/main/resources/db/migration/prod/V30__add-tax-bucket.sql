-- V30: Add tax_bucket column to t_account for financial planning categorization
ALTER TABLE public.t_account
    ADD COLUMN IF NOT EXISTS tax_bucket TEXT NULL
    CONSTRAINT ck_tax_bucket CHECK (tax_bucket IN ('pretax', 'taxable', 'roth'));
