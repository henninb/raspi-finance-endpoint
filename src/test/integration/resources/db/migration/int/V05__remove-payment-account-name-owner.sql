-- Remove obsolete account_name_owner column from t_payment (H2 integration version)
-- and enforce integrity using source_account and destination_account instead.

-- 1) Backfill destination_account from account_name_owner where needed
UPDATE int.t_payment p
SET destination_account = p.account_name_owner
WHERE (p.destination_account IS NULL OR TRIM(p.destination_account) = '')
  AND p.account_name_owner IS NOT NULL;

-- 2) Normalize destination/source to lowercase
UPDATE int.t_payment SET destination_account = LOWER(destination_account) WHERE destination_account IS NOT NULL;
UPDATE int.t_payment SET source_account = LOWER(source_account) WHERE source_account IS NOT NULL;

-- 3) Normalize hyphens to underscores if that makes them match an existing account
UPDATE int.t_payment p
SET destination_account = REPLACE(p.destination_account, '-', '_')
WHERE NOT EXISTS (
    SELECT 1 FROM int.t_account a WHERE a.account_name_owner = p.destination_account
)
AND EXISTS (
    SELECT 1 FROM int.t_account a WHERE a.account_name_owner = REPLACE(p.destination_account, '-', '_')
);

UPDATE int.t_payment p
SET source_account = REPLACE(p.source_account, '-', '_')
WHERE NOT EXISTS (
    SELECT 1 FROM int.t_account a WHERE a.account_name_owner = p.source_account
)
AND EXISTS (
    SELECT 1 FROM int.t_account a WHERE a.account_name_owner = REPLACE(p.source_account, '-', '_')
);

-- 4) Add new FKs for source_account and destination_account to t_account(account_name_owner)
-- H2 doesn't support NOT VALID, so we add them directly
ALTER TABLE int.t_payment
    ADD CONSTRAINT fk_payment_source_account FOREIGN KEY (source_account)
        REFERENCES int.t_account (account_name_owner) ON UPDATE CASCADE;

ALTER TABLE int.t_payment
    ADD CONSTRAINT fk_payment_destination_account FOREIGN KEY (destination_account)
        REFERENCES int.t_account (account_name_owner) ON UPDATE CASCADE;

-- 5) Add a new unique constraint to prevent duplicate payments per destination account/date/amount
ALTER TABLE int.t_payment
    ADD CONSTRAINT payment_constraint_destination UNIQUE (destination_account, transaction_date, amount);

-- 6) Drop old FK and column only after new constraints are in place
ALTER TABLE int.t_payment
    DROP CONSTRAINT IF EXISTS fk_account_name_owner;

ALTER TABLE int.t_payment
    DROP COLUMN IF EXISTS account_name_owner;