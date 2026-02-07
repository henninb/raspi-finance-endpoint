-- Add compound (owner, ...) FKs for t_validation_amount and t_receipt_image
-- to prevent cross-tenant references via account_id and transaction_id.

BEGIN;

-----------------------------------------------
-- 1. Add unique constraints needed for compound FKs
-----------------------------------------------
ALTER TABLE public.t_account
    ADD CONSTRAINT unique_owner_account_id UNIQUE (owner, account_id);

ALTER TABLE public.t_transaction
    ADD CONSTRAINT unique_owner_transaction_id UNIQUE (owner, transaction_id);

-----------------------------------------------
-- 2. Replace FKs with compound (owner, ...) FKs
-----------------------------------------------

-- t_validation_amount -> t_account: (owner, account_id) -> (owner, account_id)
ALTER TABLE public.t_validation_amount DROP CONSTRAINT fk_account_id;
ALTER TABLE public.t_validation_amount
    ADD CONSTRAINT fk_account_id FOREIGN KEY (owner, account_id)
        REFERENCES public.t_account (owner, account_id) ON UPDATE CASCADE;

-- t_receipt_image -> t_transaction: (owner, transaction_id) -> (owner, transaction_id)
ALTER TABLE public.t_receipt_image DROP CONSTRAINT fk_transaction;
ALTER TABLE public.t_receipt_image
    ADD CONSTRAINT fk_transaction FOREIGN KEY (owner, transaction_id)
        REFERENCES public.t_transaction (owner, transaction_id) ON UPDATE CASCADE;

COMMIT;
