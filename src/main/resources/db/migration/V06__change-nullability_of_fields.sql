ALTER TABLE t_transaction ALTER COLUMN date_updated set not null;
ALTER TABLE t_transaction ALTER COLUMN date_added set not null;
ALTER TABLE t_transaction ALTER COLUMN account_id set not null;
ALTER TABLE t_transaction ALTER COLUMN category set not null;
ALTER TABLE t_transaction ALTER COLUMN cleared set not null;