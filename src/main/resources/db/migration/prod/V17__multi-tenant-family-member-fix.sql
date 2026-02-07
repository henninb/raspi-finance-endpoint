-- Fix t_family_member multi-tenant gap:
-- 1. V09 seed data set owner to account_name_owner values (account names, not username)
-- 2. V14 fixed owner from 'henninb' to 'henninb@gmail.com' but missed t_family_member
-- 3. t_medical_expense FKs to t_family_member and t_transaction are single-column (no tenant isolation)

BEGIN;

-----------------------------------------------
-- 1. Fix owner values in t_family_member
--    V09 used account_name_owner as owner, which is wrong for multi-tenancy.
--    All existing family members belong to the only existing user.
-----------------------------------------------
UPDATE public.t_family_member
SET owner = 'henninb@gmail.com'
WHERE owner IS NULL OR owner = '' OR owner != 'henninb@gmail.com';

-----------------------------------------------
-- 2. Add unique constraint on (owner, family_member_id) for compound FK target
-----------------------------------------------
ALTER TABLE public.t_family_member
    ADD CONSTRAINT unique_owner_family_member_id UNIQUE (owner, family_member_id);

-----------------------------------------------
-- 3. Replace single-column FK on t_medical_expense -> t_family_member
--    with compound (owner, family_member_id) FK
-----------------------------------------------
ALTER TABLE public.t_medical_expense DROP CONSTRAINT fk_medical_expense_family_member;
ALTER TABLE public.t_medical_expense
    ADD CONSTRAINT fk_medical_expense_family_member FOREIGN KEY (owner, family_member_id)
        REFERENCES public.t_family_member (owner, family_member_id) ON UPDATE CASCADE;

-----------------------------------------------
-- 4. Replace single-column FK on t_medical_expense -> t_transaction
--    with compound (owner, transaction_id) FK
--    (V16 added unique_owner_transaction_id on t_transaction but only
--     updated t_receipt_image, not t_medical_expense)
-----------------------------------------------
ALTER TABLE public.t_medical_expense DROP CONSTRAINT fk_medical_expense_transaction;
ALTER TABLE public.t_medical_expense
    ADD CONSTRAINT fk_medical_expense_transaction FOREIGN KEY (owner, transaction_id)
        REFERENCES public.t_transaction (owner, transaction_id) ON DELETE CASCADE;

COMMIT;
