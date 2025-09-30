-- Drop obsolete foreign key on t_payment(account_name_owner) (H2 integration version)
-- Safe even if already removed

ALTER TABLE int.t_payment
    DROP CONSTRAINT IF EXISTS fk_account_name_owner;