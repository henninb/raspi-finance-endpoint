-- Drop obsolete foreign key on t_payment(account_name_owner)
-- Safe even if already removed

ALTER TABLE public.t_payment
    DROP CONSTRAINT IF EXISTS fk_account_name_owner;

