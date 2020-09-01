-------------------------------------------------------------------------------
-- CREATE the objects that are not entities
-- SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'T_ACCOUNT';
-- SELECT * FROM INFORMATION_SCHEMA.CONSTRAINTS WHERE TABLE_NAME = 'T_ACCOUNT';
-------------------------------------------------------------------------------

-- CONSTRAINT unique_account_name_owner_account_id UNIQUE (account_id, account_name_owner, account_type),
-- CONSTRAINT t_account_account_name_owner_lowercase_ck CHECK (account_name_owner = lower(account_name_owner)),
-- CONSTRAINT t_account_account_type_lowercase_ck CHECK (account_type = lower(account_type))

-- ALTER TABLE t_account ADD CONSTRAINT TEST_UNIQUE UNIQUE ( account_id, account_name_owner, account_type);
-- insert into t_account(account_name_owner, account_type, active_status,date_added, date_updated) VALUES('cash_brian', 'debit', true, now(), now());

-- ALTER TABLE t_account ADD CONSTRAINT unique_account_name_owner_account_id UNIQUE(account_id, account_name_owner, account_type);
-- ALTER TABLE t_account ADD CONSTRAINT t_account_account_type_lowercase_ck CHECK (account_type = lower(account_type));

--select * from t_transaction;
--insert into t_transaction(account_id, account_name_owner, account_type, amount, category, cleared, description, guid, notes, reoccurring, transaction_date) VALUES (1, 'TEST_brian', 'credit', 0.0, '', 1, 'testing', '031a8d6a-27c2-4302-bb6d-60dc64400081', '', true, '2020-01-12');

CREATE TABLE IF NOT EXISTS t_transaction_categories
(
    category_id    BIGINT NOT NULL,
    transaction_id BIGINT NOT NULL,
    date_updated   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_added     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
