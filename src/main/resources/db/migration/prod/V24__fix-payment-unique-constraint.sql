-- Replace the payment uniqueness constraint to include source_account.
-- The old constraint (owner, destination_account, transaction_date, amount) incorrectly
-- blocked two payments from different source accounts to the same destination on the same
-- day for the same amount, which are distinct legitimate operations.
ALTER TABLE public.t_payment
    DROP CONSTRAINT IF EXISTS unique_owner_payment;

ALTER TABLE public.t_payment
    ADD CONSTRAINT unique_owner_payment
        UNIQUE (owner, source_account, destination_account, transaction_date, amount);
