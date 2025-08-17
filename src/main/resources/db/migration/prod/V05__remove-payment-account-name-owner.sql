-- Remove obsolete account_name_owner column from t_payment
-- and enforce integrity using source_account and destination_account instead.

BEGIN;

-- 1) Backfill destination_account from account_name_owner where needed
UPDATE public.t_payment p
SET destination_account = p.account_name_owner
WHERE (p.destination_account IS NULL OR btrim(p.destination_account) = '')
  AND p.account_name_owner IS NOT NULL;

-- 2) Normalize destination/source to lowercase
UPDATE public.t_payment p SET destination_account = lower(p.destination_account) WHERE p.destination_account IS NOT NULL;
UPDATE public.t_payment p SET source_account = lower(p.source_account) WHERE p.source_account IS NOT NULL;

-- 3) Normalize hyphens to underscores if that makes them match an existing account
UPDATE public.t_payment p
SET destination_account = REPLACE(p.destination_account, '-', '_')
WHERE NOT EXISTS (
    SELECT 1 FROM public.t_account a WHERE a.account_name_owner = p.destination_account
)
AND EXISTS (
    SELECT 1 FROM public.t_account a WHERE a.account_name_owner = REPLACE(p.destination_account, '-', '_')
);

UPDATE public.t_payment p
SET source_account = REPLACE(p.source_account, '-', '_')
WHERE NOT EXISTS (
    SELECT 1 FROM public.t_account a WHERE a.account_name_owner = p.source_account
)
AND EXISTS (
    SELECT 1 FROM public.t_account a WHERE a.account_name_owner = REPLACE(p.source_account, '-', '_')
);

-- Add FKs for source_account and destination_account to t_account(account_name_owner)
-- 4) Add new FKs as NOT VALID first, then validate after data cleanup
ALTER TABLE public.t_payment
    ADD CONSTRAINT fk_payment_source_account FOREIGN KEY (source_account)
        REFERENCES public.t_account (account_name_owner) ON UPDATE CASCADE NOT VALID;

ALTER TABLE public.t_payment
    ADD CONSTRAINT fk_payment_destination_account FOREIGN KEY (destination_account)
        REFERENCES public.t_account (account_name_owner) ON UPDATE CASCADE NOT VALID;

-- Validate constraints (will check all existing rows)
ALTER TABLE public.t_payment VALIDATE CONSTRAINT fk_payment_source_account;
ALTER TABLE public.t_payment VALIDATE CONSTRAINT fk_payment_destination_account;

-- Add a new unique constraint to prevent duplicate payments per destination account/date/amount
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'payment_constraint_destination'
    ) THEN
        ALTER TABLE public.t_payment
            ADD CONSTRAINT payment_constraint_destination UNIQUE (destination_account, transaction_date, amount);
    END IF;
END $$;

-- 6) Drop old FK and column only after new constraints are in place
ALTER TABLE public.t_payment
    DROP CONSTRAINT IF EXISTS fk_account_name_owner;

ALTER TABLE public.t_payment
    DROP COLUMN IF EXISTS account_name_owner;

COMMIT;
