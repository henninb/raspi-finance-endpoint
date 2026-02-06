-- Multi-Tenant Stage 1: Database-only migration
-- Backfill owner columns, add owner to t_medical_expense,
-- add owner indexes, add composite unique constraints alongside existing ones.
-- Zero impact on running application since it ignores owner columns.

BEGIN;

---------------------------------------
-- 1. Backfill owner columns with 'henninb'
---------------------------------------
UPDATE public.t_account SET owner = 'henninb' WHERE owner IS NULL;
UPDATE public.t_transaction SET owner = 'henninb' WHERE owner IS NULL;
UPDATE public.t_category SET owner = 'henninb' WHERE owner IS NULL;
UPDATE public.t_description SET owner = 'henninb' WHERE owner IS NULL;
UPDATE public.t_payment SET owner = 'henninb' WHERE owner IS NULL;
UPDATE public.t_transfer SET owner = 'henninb' WHERE owner IS NULL;
UPDATE public.t_validation_amount SET owner = 'henninb' WHERE owner IS NULL;
UPDATE public.t_receipt_image SET owner = 'henninb' WHERE owner IS NULL;
UPDATE public.t_pending_transaction SET owner = 'henninb' WHERE owner IS NULL;
UPDATE public.t_parameter SET owner = 'henninb' WHERE owner IS NULL;
UPDATE public.t_transaction_categories SET owner = 'henninb' WHERE owner IS NULL;

---------------------------------------
-- 2. Add owner column to t_medical_expense
---------------------------------------
ALTER TABLE public.t_medical_expense ADD COLUMN IF NOT EXISTS owner TEXT NULL;
UPDATE public.t_medical_expense SET owner = 'henninb' WHERE owner IS NULL;

---------------------------------------
-- 3. Add indexes on owner
---------------------------------------
CREATE INDEX IF NOT EXISTS idx_account_owner ON public.t_account(owner);
CREATE INDEX IF NOT EXISTS idx_transaction_owner ON public.t_transaction(owner);
CREATE INDEX IF NOT EXISTS idx_category_owner ON public.t_category(owner);
CREATE INDEX IF NOT EXISTS idx_description_owner ON public.t_description(owner);
CREATE INDEX IF NOT EXISTS idx_payment_owner ON public.t_payment(owner);
CREATE INDEX IF NOT EXISTS idx_transfer_owner ON public.t_transfer(owner);
CREATE INDEX IF NOT EXISTS idx_validation_amount_owner ON public.t_validation_amount(owner);
CREATE INDEX IF NOT EXISTS idx_receipt_image_owner ON public.t_receipt_image(owner);
CREATE INDEX IF NOT EXISTS idx_pending_transaction_owner ON public.t_pending_transaction(owner);
CREATE INDEX IF NOT EXISTS idx_parameter_owner ON public.t_parameter(owner);
CREATE INDEX IF NOT EXISTS idx_transaction_categories_owner ON public.t_transaction_categories(owner);
CREATE INDEX IF NOT EXISTS idx_medical_expense_owner ON public.t_medical_expense(owner);

---------------------------------------
-- 4. Add composite unique constraints alongside existing ones
---------------------------------------

-- t_account: existing unique_account_name_owner_account_type(account_name_owner, account_type)
ALTER TABLE public.t_account
    ADD CONSTRAINT unique_owner_account_name_owner_account_type UNIQUE (owner, account_name_owner, account_type);

-- t_category: existing category_name UNIQUE
ALTER TABLE public.t_category
    ADD CONSTRAINT unique_owner_category_name UNIQUE (owner, category_name);

-- t_description: existing description_name UNIQUE
ALTER TABLE public.t_description
    ADD CONSTRAINT unique_owner_description_name UNIQUE (owner, description_name);

-- t_transaction: existing transaction_constraint(account_name_owner, transaction_date, description, category, amount, notes)
ALTER TABLE public.t_transaction
    ADD CONSTRAINT unique_owner_transaction UNIQUE (owner, account_name_owner, transaction_date, description, category, amount, notes);

-- t_payment: existing payment_constraint_destination(destination_account, transaction_date, amount) from V05
ALTER TABLE public.t_payment
    ADD CONSTRAINT unique_owner_payment UNIQUE (owner, destination_account, transaction_date, amount);

-- t_transfer: existing transfer_constraint(source_account, destination_account, transaction_date, amount)
ALTER TABLE public.t_transfer
    ADD CONSTRAINT unique_owner_transfer UNIQUE (owner, source_account, destination_account, transaction_date, amount);

-- t_parameter: existing parameter_name UNIQUE
ALTER TABLE public.t_parameter
    ADD CONSTRAINT unique_owner_parameter_name UNIQUE (owner, parameter_name);

-- t_pending_transaction: existing unique_pending_transaction_fields(account_name_owner, transaction_date, description, amount)
ALTER TABLE public.t_pending_transaction
    ADD CONSTRAINT unique_owner_pending_transaction UNIQUE (owner, account_name_owner, transaction_date, description, amount);

COMMIT;
