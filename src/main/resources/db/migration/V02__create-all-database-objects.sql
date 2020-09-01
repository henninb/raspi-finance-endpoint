CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SET client_min_messages TO WARNING;

-------------
-- Account --
-------------
CREATE SEQUENCE IF NOT EXISTS seq_account_id START WITH 1001;

CREATE TABLE IF NOT EXISTS t_account
(
    account_id         BIGINT      NOT NULL DEFAULT nextval(seq_account_id),
    account_name_owner TEXT UNIQUE NOT NULL,
    account_name       TEXT, -- NULL for now
    account_owner      TEXT, -- NULL for now
    account_type       TEXT        NOT NULL DEFAULT 'unknown',
    active_status      BOOLEAN     NOT NULL DEFAULT TRUE,
    moniker            TEXT        NOT NULL DEFAULT '0000',
    totals             DECIMAL(12, 2)       DEFAULT 0.0,
    totals_balanced    DECIMAL(12, 2)       DEFAULT 0.0,
    date_closed        TIMESTAMP            DEFAULT TO_TIMESTAMP(0),
    date_updated       TIMESTAMP            DEFAULT TO_TIMESTAMP(0),
    date_added         TIMESTAMP            DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT pk_account_id PRIMARY KEY (account_id),
    CONSTRAINT unique_account_name_owner_account_id UNIQUE (account_id, account_name_owner, account_type),
    CONSTRAINT ck_account_name_owner_lowercase CHECK (account_name_owner = lower(account_name_owner)),
    CONSTRAINT ck_account_type_lowercase CHECK (account_type = lower(account_type))
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
CREATE TRIGGER tr_upd_ts_account
    BEFORE UPDATE
    ON t_account
    FOR EACH ROW
EXECUTE PROCEDURE fn_upd_ts_account();

CREATE OR REPLACE FUNCTION fn_ins_ts_account() RETURNS TRIGGER AS
$$
BEGIN
    RAISE NOTICE 'fn_ins_ts_account';
    NEW.date_added := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_ins_ts_account ON t_account;
CREATE TRIGGER tr_ins_ts_account
    BEFORE INSERT
    ON t_account
    FOR EACH ROW
EXECUTE PROCEDURE fn_ins_ts_account();

--------------
-- Category --
--------------
CREATE SEQUENCE IF NOT EXISTS seq_category_id start with 1001;

CREATE TABLE IF NOT EXISTS t_category
(
    category_id  BIGINT      NOT NULL DEFAULT nextval(seq_category_id),
    category     TEXT UNIQUE NOT NULL,
    date_updated TIMESTAMP   NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added   TIMESTAMP   NOT NULL DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT pk_category_id PRIMARY KEY (category_id)
);

---------------------------
-- TransactionCategories --
---------------------------

CREATE TABLE IF NOT EXISTS t_transaction_categories
(
    category_id    BIGINT    NOT NULL,
    transaction_id BIGINT    NOT NULL,
    date_updated   TIMESTAMP NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added     TIMESTAMP NOT NULL DEFAULT TO_TIMESTAMP(0),
    PRIMARY KEY (category_id, transaction_id)
);

-----------------
-- Transaction --
-----------------
--CREATE TYPE transaction_state_enum AS ENUM ('outstanding','future','cleared', 'undefined');
--CREATE TYPE account_type_enum AS ENUM ('credit','debit', 'undefined');

CREATE SEQUENCE IF NOT EXISTS seq_transaction_id start with 1001;

CREATE TABLE IF NOT EXISTS t_transaction
(
    transaction_id     BIGINT         NOT NULL DEFAULT nextval(seq_transaction_id),
    account_id         BIGINT         NOT NULL DEFAULT -1,
    account_type       TEXT           NOT NULL DEFAULT 'undefined',
    account_name_owner TEXT           NOT NULL,
    guid               TEXT           NOT NULL UNIQUE,
    transaction_date   DATE           NOT NULL,
    description        TEXT           NOT NULL,
    category           TEXT           NOT NULL DEFAULT '',
    amount             DECIMAL(12, 2) NOT NULL DEFAULT 0.0,
    transaction_state  TEXT           NOT NULL DEFAULT 'undefined',
    reoccurring        BOOLEAN        NOT NULL DEFAULT FALSE,
    active_status      BOOLEAN        NOT NULL DEFAULT TRUE,
    notes              TEXT           NOT NULL DEFAULT '',
    date_updated       TIMESTAMP      NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added         TIMESTAMP      NOT NULL DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT pk_transaction_id PRIMARY KEY (transaction_id),
    CONSTRAINT transaction_constraint UNIQUE (account_name_owner, transaction_date, description, category, amount,
                                              notes),
    CONSTRAINT t_transaction_description_lowercase_ck CHECK (description = lower(description)),
    CONSTRAINT t_transaction_category_lowercase_ck CHECK (category = lower(category)),
    CONSTRAINT t_transaction_notes_lowercase_ck CHECK (notes = lower(notes)),
    --CONSTRAINT t_transaction_account_type_lowercase_ck CHECK (account_type = lower(account_type)),
    --CONSTRAINT fk_category_id_transaction_id FOREIGN KEY(transaction_id) REFERENCES t_transaction_categories(category_id, transaction_id) ON DELETE CASCADE,
    CONSTRAINT ck_transaction_state CHECK (transaction_state IN ('outstanding', 'future', 'cleared', 'undefined')),
    CONSTRAINT ck_account_type CHECK (account_type IN ('debit', 'credit', 'undefined')),
    CONSTRAINT fk_account_id_account_name_owner FOREIGN KEY (account_id, account_name_owner, account_type) REFERENCES t_account (account_id, account_name_owner, account_type) ON DELETE CASCADE
);

--ALTER TABLE t_transaction ADD CONSTRAINT ck_transaction_state CHECK (transaction_state IN ('outstanding', 'future', 'cleared', 'undefined'));
--ALTER TABLE t_transaction ADD CONSTRAINT ck_account_type CHECK (account_type IN ('debit', 'credit', 'undefined'));

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
CREATE TRIGGER tr_ins_ts_transactions
    BEFORE INSERT
    ON t_transaction
    FOR EACH ROW
EXECUTE PROCEDURE fn_ins_ts_transaction();

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
CREATE TRIGGER tr_upd_ts_transactions
    BEFORE UPDATE
    ON t_transaction
    FOR EACH ROW
EXECUTE PROCEDURE fn_upd_ts_transaction();

-------------
-- Payment --
-------------
CREATE SEQUENCE IF NOT EXISTS seq_payment_id START WITH 1001;

CREATE TABLE IF NOT EXISTS t_payment
(
    payment_id         BIGINT         NOT NULL DEFAULT nextval(seq_payment_id),
    account_name_owner TEXT           NOT NULL,
    transaction_date   DATE           NOT NULL,
    amount             DECIMAL(12, 2) NOT NULL DEFAULT 0.0,
    guid_source        TEXT           NOT NULL,
    guid_destination   TEXT           NOT NULL,
    date_updated       TIMESTAMP               DEFAULT TO_TIMESTAMP(0),
    date_added         TIMESTAMP               DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT pk_payment_id PRIMARY KEY (payment_id),
    CONSTRAINT payment_constraint UNIQUE (account_name_owner, transaction_date, amount),
    CONSTRAINT fk_guid_source FOREIGN KEY (guid_source) REFERENCES t_transaction (guid),
    CONSTRAINT fk_guid_destination FOREIGN KEY (guid_destination) REFERENCES t_transaction (guid)
);

-- check for locks
-- SELECT pid, usename, pg_blocking_pids(pid) as blocked_by, query as blocked_query from pg_stat_activity where cardinality(pg_blocking_pids(pid)) > 0;
