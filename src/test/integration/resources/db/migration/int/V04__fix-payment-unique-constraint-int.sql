ALTER TABLE int.t_payment DROP CONSTRAINT IF EXISTS unique_owner_payment;
ALTER TABLE int.t_payment ADD CONSTRAINT unique_owner_payment UNIQUE (owner, source_account, destination_account, transaction_date, amount);
