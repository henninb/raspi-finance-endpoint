CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SET client_min_messages TO WARNING;

-------------
-- Account --
-------------
CREATE SEQUENCE IF NOT EXISTS t_account_account_id_seq START WITH 1001;

CREATE TABLE IF NOT EXISTS t_account(
    account_id BIGINT DEFAULT nextval('t_account_account_id_seq') NOT NULL,
    account_name_owner TEXT UNIQUE NOT NULL,
    account_name TEXT, -- NULL for now
    account_owner TEXT, -- NULL for now
    account_type TEXT NOT NULL DEFAULT 'unknown',
    active_status BOOLEAN NOT NULL DEFAULT TRUE,
    moniker TEXT NOT NULL DEFAULT '0000',
    totals DECIMAL(12,2) DEFAULT 0.0,
    totals_balanced DECIMAL(12,2) DEFAULT 0.0,
    date_closed TIMESTAMP DEFAULT TO_TIMESTAMP(0),
    date_updated TIMESTAMP DEFAULT TO_TIMESTAMP(0),
    date_added TIMESTAMP DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT pk_account_id PRIMARY KEY (account_id),
    CONSTRAINT unique_account_name_owner_account_id UNIQUE (account_id, account_name_owner, account_type),
    CONSTRAINT t_account_account_name_owner_lowercase_ck CHECK (account_name_owner = lower(account_name_owner)),
    CONSTRAINT t_account_account_type_lowercase_ck CHECK (account_type = lower(account_type))
);

CREATE OR REPLACE FUNCTION fn_upd_ts_account() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    RAISE NOTICE 'fn_upd_ts_account';
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_upd_ts_account ON t_account;
CREATE TRIGGER tr_upd_ts_account BEFORE UPDATE ON t_account FOR EACH ROW EXECUTE PROCEDURE fn_upd_ts_account();

CREATE OR REPLACE FUNCTION fn_ins_ts_account() RETURNS TRIGGER AS
$$
BEGIN
    RAISE NOTICE 'fn_ins_ts_account';
    NEW.date_added := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_ins_ts_account ON t_account;
CREATE TRIGGER tr_ins_ts_account BEFORE INSERT ON t_account FOR EACH ROW EXECUTE PROCEDURE fn_ins_ts_account();

--------------
-- Category --
--------------
CREATE SEQUENCE IF NOT EXISTS t_category_category_id_seq start with 1001;

CREATE TABLE IF NOT EXISTS t_category(
    category_id BIGINT DEFAULT nextval('t_category_category_id_seq') NOT NULL,
    category TEXT UNIQUE NOT NULL,
    date_updated TIMESTAMP NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added TIMESTAMP NOT NULL DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT pk_category_id PRIMARY KEY (category_id)
);

---------------------------
-- TransactionCategories --
---------------------------

CREATE TABLE IF NOT EXISTS t_transaction_categories(
    category_id BIGINT NOT NULL,
    transaction_id BIGINT NOT NULL,
    date_updated TIMESTAMP NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added TIMESTAMP NOT NULL DEFAULT TO_TIMESTAMP(0)
);

CREATE SEQUENCE IF NOT EXISTS t_transaction_transaction_id_seq start with 1001;

-----------------
-- Transaction --
-----------------
CREATE TABLE IF NOT EXISTS t_transaction (
    transaction_id BIGINT DEFAULT nextval('t_transaction_transaction_id_seq') NOT NULL,
    account_id BIGINT NOT NULL DEFAULT -1,
    account_type TEXT NOT NULL,
    account_name_owner TEXT NOT NULL,
    guid TEXT NOT NULL UNIQUE,
    transaction_date DATE NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT '',
    amount DECIMAL(12,2) NOT NULL DEFAULT 0.0,
    cleared INTEGER NOT NULL DEFAULT 0,
    reoccurring BOOLEAN NOT NULL DEFAULT FALSE,
    active_status BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT NOT NULL DEFAULT '',
    date_updated TIMESTAMP NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added TIMESTAMP NOT NULL DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT pk_transaction_id PRIMARY KEY (transaction_id),
    CONSTRAINT transaction_constraint UNIQUE (account_name_owner, transaction_date, description, category, amount, notes),
    CONSTRAINT t_transaction_description_lowercase_ck CHECK (description = lower(description)),
    CONSTRAINT t_transaction_category_lowercase_ck CHECK (category = lower(category)),
    CONSTRAINT t_transaction_notes_lowercase_ck CHECK (notes = lower(notes)),
    CONSTRAINT t_transaction_account_type_lowercase_ck CHECK (account_type = lower(account_type)),
    CONSTRAINT fk_account_id_account_name_owner FOREIGN KEY(account_id, account_name_owner, account_type) REFERENCES t_account(account_id, account_name_owner, account_type)
);

CREATE OR REPLACE FUNCTION fn_ins_ts_transaction() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    --TODO: add logic to insert account
    NEW.date_added := CURRENT_TIMESTAMP;
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_ins_ts_transactions ON t_transaction;
CREATE TRIGGER tr_ins_ts_transactions BEFORE INSERT ON t_transaction FOR EACH ROW EXECUTE PROCEDURE fn_ins_ts_transaction();

CREATE OR REPLACE FUNCTION fn_upd_ts_transaction() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    RAISE NOTICE 'fn_upd_ts_transaction';
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_upd_ts_transactions ON t_transaction;
CREATE TRIGGER tr_upd_ts_transactions BEFORE UPDATE ON t_transaction FOR EACH ROW EXECUTE PROCEDURE fn_upd_ts_transaction();

-------------
-- Payment --
-------------
CREATE SEQUENCE IF NOT EXISTS t_payment_payment_id_seq START WITH 1001;

CREATE TABLE IF NOT EXISTS t_payment(
    payment_id BIGINT DEFAULT nextval('t_payment_payment_id_seq') NOT NULL,
    account_name_owner TEXT NOT NULL,
    transaction_date DATE NOT NULL,
    amount DECIMAL(12,2) NOT NULL DEFAULT 0.0,
    guid_source TEXT NOT NULL,
    guid_destination TEXT NOT NULL,
    date_updated TIMESTAMP DEFAULT TO_TIMESTAMP(0),
    date_added TIMESTAMP DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT pk_payment_id PRIMARY KEY (payment_id),
    CONSTRAINT payment_constraint UNIQUE (account_name_owner, transaction_date, amount),
    CONSTRAINT fk_guid_source FOREIGN KEY(guid_source) REFERENCES t_transaction(guid),
    CONSTRAINT fk_guid_destination FOREIGN KEY(guid_destination) REFERENCES t_transaction(guid)
);
