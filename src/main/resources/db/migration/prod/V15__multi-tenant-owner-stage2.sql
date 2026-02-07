-- Multi-Tenant Stage 2: Enforce owner at database level
-- 1. Make owner NOT NULL on all tables
-- 2. Add compound unique constraints needed for new FKs
-- 3. Replace single-column FKs with compound (owner, ...) FKs
-- 4. Drop old global unique constraints (allows same names across tenants)
-- 5. Update stored functions to include owner parameter

BEGIN;

---------------------------------------
-- 0. Backfill any remaining NULL or empty owner rows (inserted after V13/V14)
---------------------------------------
UPDATE public.t_account SET owner = 'henninb@gmail.com' WHERE owner IS NULL OR owner = '';
UPDATE public.t_transaction SET owner = 'henninb@gmail.com' WHERE owner IS NULL OR owner = '';
UPDATE public.t_category SET owner = 'henninb@gmail.com' WHERE owner IS NULL OR owner = '';
UPDATE public.t_description SET owner = 'henninb@gmail.com' WHERE owner IS NULL OR owner = '';
UPDATE public.t_payment SET owner = 'henninb@gmail.com' WHERE owner IS NULL OR owner = '';
UPDATE public.t_transfer SET owner = 'henninb@gmail.com' WHERE owner IS NULL OR owner = '';
UPDATE public.t_validation_amount SET owner = 'henninb@gmail.com' WHERE owner IS NULL OR owner = '';
UPDATE public.t_receipt_image SET owner = 'henninb@gmail.com' WHERE owner IS NULL OR owner = '';
UPDATE public.t_pending_transaction SET owner = 'henninb@gmail.com' WHERE owner IS NULL OR owner = '';
UPDATE public.t_parameter SET owner = 'henninb@gmail.com' WHERE owner IS NULL OR owner = '';
UPDATE public.t_transaction_categories SET owner = 'henninb@gmail.com' WHERE owner IS NULL OR owner = '';
UPDATE public.t_medical_expense SET owner = 'henninb@gmail.com' WHERE owner IS NULL OR owner = '';

---------------------------------------
-- 1. Make owner NOT NULL on all tables
---------------------------------------
ALTER TABLE public.t_account ALTER COLUMN owner SET NOT NULL;
ALTER TABLE public.t_transaction ALTER COLUMN owner SET NOT NULL;
ALTER TABLE public.t_category ALTER COLUMN owner SET NOT NULL;
ALTER TABLE public.t_description ALTER COLUMN owner SET NOT NULL;
ALTER TABLE public.t_payment ALTER COLUMN owner SET NOT NULL;
ALTER TABLE public.t_transfer ALTER COLUMN owner SET NOT NULL;
ALTER TABLE public.t_validation_amount ALTER COLUMN owner SET NOT NULL;
ALTER TABLE public.t_receipt_image ALTER COLUMN owner SET NOT NULL;
ALTER TABLE public.t_pending_transaction ALTER COLUMN owner SET NOT NULL;
ALTER TABLE public.t_parameter ALTER COLUMN owner SET NOT NULL;
ALTER TABLE public.t_transaction_categories ALTER COLUMN owner SET NOT NULL;
ALTER TABLE public.t_medical_expense ALTER COLUMN owner SET NOT NULL;

-----------------------------------------------
-- 2. Add compound unique constraints for FKs
-----------------------------------------------
-- t_account needs (owner, account_name_owner) for FKs from payment, transfer, pending_transaction
ALTER TABLE public.t_account
    ADD CONSTRAINT unique_owner_account_name_owner UNIQUE (owner, account_name_owner);

-- t_account needs (owner, account_id, account_name_owner, account_type) for FK from transaction
ALTER TABLE public.t_account
    ADD CONSTRAINT unique_owner_account_id_name_type UNIQUE (owner, account_id, account_name_owner, account_type);

-----------------------------------------------
-- 2b. Drop old FKs that depend on old unique constraints
-----------------------------------------------
ALTER TABLE public.t_transaction DROP CONSTRAINT fk_category_name;
ALTER TABLE public.t_transaction DROP CONSTRAINT fk_description_name;

-----------------------------------------------
-- 2c. Drop old global unique constraints on category and description
--     so we can insert owner-scoped rows without conflicts
-----------------------------------------------
ALTER TABLE public.t_category DROP CONSTRAINT t_category_category_name_key;
ALTER TABLE public.t_description DROP CONSTRAINT t_description_description_name_key;

-----------------------------------------------
-- 2d. Insert missing categories and descriptions referenced by transactions
-----------------------------------------------
INSERT INTO public.t_category (category_name, owner, active_status, date_updated, date_added)
SELECT DISTINCT t.category, t.owner, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM public.t_transaction t
WHERE NOT EXISTS (
    SELECT 1 FROM public.t_category c
    WHERE c.category_name = t.category AND c.owner = t.owner
);

INSERT INTO public.t_description (description_name, owner, active_status, date_updated, date_added)
SELECT DISTINCT t.description, t.owner, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM public.t_transaction t
WHERE NOT EXISTS (
    SELECT 1 FROM public.t_description d
    WHERE d.description_name = t.description AND d.owner = t.owner
);

-----------------------------------------------
-- 3. Add compound (owner, ...) FKs
-----------------------------------------------

-- t_transaction -> t_category: (owner, category) -> (owner, category_name)
ALTER TABLE public.t_transaction
    ADD CONSTRAINT fk_category_name FOREIGN KEY (owner, category)
        REFERENCES public.t_category (owner, category_name) ON UPDATE CASCADE ON DELETE RESTRICT;

-- t_transaction -> t_description: (owner, description) -> (owner, description_name)
ALTER TABLE public.t_transaction
    ADD CONSTRAINT fk_description_name FOREIGN KEY (owner, description)
        REFERENCES public.t_description (owner, description_name) ON UPDATE CASCADE ON DELETE RESTRICT;

-- t_transaction -> t_account: add owner to the compound FK
ALTER TABLE public.t_transaction DROP CONSTRAINT fk_account_id_account_name_owner;
ALTER TABLE public.t_transaction
    ADD CONSTRAINT fk_account_id_account_name_owner FOREIGN KEY (owner, account_id, account_name_owner, account_type)
        REFERENCES public.t_account (owner, account_id, account_name_owner, account_type) ON UPDATE CASCADE;

-- t_pending_transaction -> t_account: (owner, account_name_owner) -> (owner, account_name_owner)
ALTER TABLE public.t_pending_transaction DROP CONSTRAINT fk_pending_account;
ALTER TABLE public.t_pending_transaction
    ADD CONSTRAINT fk_pending_account FOREIGN KEY (owner, account_name_owner)
        REFERENCES public.t_account (owner, account_name_owner) ON UPDATE CASCADE;

-- t_payment -> t_account: source and destination compound FKs
ALTER TABLE public.t_payment DROP CONSTRAINT fk_payment_source_account;
ALTER TABLE public.t_payment
    ADD CONSTRAINT fk_payment_source_account FOREIGN KEY (owner, source_account)
        REFERENCES public.t_account (owner, account_name_owner) ON UPDATE CASCADE;

ALTER TABLE public.t_payment DROP CONSTRAINT fk_payment_destination_account;
ALTER TABLE public.t_payment
    ADD CONSTRAINT fk_payment_destination_account FOREIGN KEY (owner, destination_account)
        REFERENCES public.t_account (owner, account_name_owner) ON UPDATE CASCADE;

-- t_transfer -> t_account: source and destination compound FKs
ALTER TABLE public.t_transfer DROP CONSTRAINT fk_source_account;
ALTER TABLE public.t_transfer
    ADD CONSTRAINT fk_source_account FOREIGN KEY (owner, source_account)
        REFERENCES public.t_account (owner, account_name_owner) ON UPDATE CASCADE;

ALTER TABLE public.t_transfer DROP CONSTRAINT fk_destination_account;
ALTER TABLE public.t_transfer
    ADD CONSTRAINT fk_destination_account FOREIGN KEY (owner, destination_account)
        REFERENCES public.t_account (owner, account_name_owner) ON UPDATE CASCADE;

-----------------------------------------------
-- 4. Drop old global unique constraints
-----------------------------------------------
-- These prevented the same name from existing across different tenants.
-- The new owner-scoped constraints from V13 replace them.

-- t_account: drop global uniques (account_name_owner alone, and without owner)
ALTER TABLE public.t_account DROP CONSTRAINT t_account_account_name_owner_key;
ALTER TABLE public.t_account DROP CONSTRAINT unique_account_name_owner_account_type;
ALTER TABLE public.t_account DROP CONSTRAINT unique_account_name_owner_account_id;

-- t_category and t_description global uniques already dropped in step 2b

-- t_transaction: drop global unique constraint
ALTER TABLE public.t_transaction DROP CONSTRAINT transaction_constraint;

-- t_payment: drop global unique constraint (from V05)
ALTER TABLE public.t_payment DROP CONSTRAINT payment_constraint_destination;

-- t_transfer: drop global unique constraint
ALTER TABLE public.t_transfer DROP CONSTRAINT transfer_constraint;

-- t_parameter: drop global unique on parameter_name
ALTER TABLE public.t_parameter DROP CONSTRAINT t_parameter_parameter_name_key;

-- t_pending_transaction: drop global unique constraint
ALTER TABLE public.t_pending_transaction DROP CONSTRAINT unique_pending_transaction_fields;

-----------------------------------------------
-- 5. Update stored functions with owner param
-----------------------------------------------
CREATE OR REPLACE FUNCTION rename_account_owner(
    p_old_name VARCHAR,
    p_new_name VARCHAR,
    p_owner VARCHAR
)
RETURNS VOID
SET SCHEMA 'public'
LANGUAGE PLPGSQL
AS
$$
BEGIN
    EXECUTE 'ALTER TABLE t_transaction DISABLE TRIGGER ALL';

    EXECUTE 'UPDATE t_transaction SET account_name_owner = $1 WHERE account_name_owner = $2 AND owner = $3'
    USING p_new_name, p_old_name, p_owner;

    EXECUTE 'UPDATE t_account SET account_name_owner = $1 WHERE account_name_owner = $2 AND owner = $3'
    USING p_new_name, p_old_name, p_owner;

    EXECUTE 'ALTER TABLE t_transaction ENABLE TRIGGER ALL';
END;
$$;

CREATE OR REPLACE FUNCTION disable_account_owner(
    p_new_name VARCHAR,
    p_owner VARCHAR
)
RETURNS VOID
SET SCHEMA 'public'
LANGUAGE PLPGSQL
AS
$$
BEGIN
    EXECUTE 'ALTER TABLE t_transaction DISABLE TRIGGER ALL';

    EXECUTE 'UPDATE t_transaction SET active_status = false WHERE account_name_owner = $1 AND owner = $2'
    USING p_new_name, p_owner;

    EXECUTE 'UPDATE t_account SET active_status = false WHERE account_name_owner = $1 AND owner = $2'
    USING p_new_name, p_owner;

    EXECUTE 'ALTER TABLE t_transaction ENABLE TRIGGER ALL';
END;
$$;

COMMIT;
