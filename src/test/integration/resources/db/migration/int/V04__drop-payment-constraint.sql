-- Drop payment_constraint from t_payment table (H2 integration version)
-- This constraint ensures uniqueness based on account_name_owner, transaction_date, and amount
-- Removing this to allow duplicate payments with same details

ALTER TABLE int.t_payment DROP CONSTRAINT payment_constraint;