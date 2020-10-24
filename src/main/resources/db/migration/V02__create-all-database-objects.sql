CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SET client_min_messages TO WARNING;

-------------
-- Account --
-------------
CREATE TABLE IF NOT EXISTS t_account
(
    account_id         BIGSERIAL PRIMARY KEY,
    account_name_owner TEXT UNIQUE NOT NULL,
    account_name       TEXT, -- NULL for now
    account_owner      TEXT, -- NULL for now
    account_type       TEXT        NOT NULL DEFAULT 'unknown',
    active_status      BOOLEAN     NOT NULL DEFAULT TRUE,
    moniker            TEXT        NOT NULL DEFAULT '0000',
    totals             DECIMAL(12, 2)       DEFAULT 0.0,
    totals_balanced    DECIMAL(12, 2)       DEFAULT 0.0,
    date_closed        TIMESTAMP            DEFAULT TO_TIMESTAMP(0),
    date_updated       TIMESTAMP   NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added         TIMESTAMP   NOT NULL DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT unique_account_name_owner_account_id UNIQUE (account_id, account_name_owner, account_type),
    CONSTRAINT unique_account_name_owner_account_type UNIQUE (account_name_owner, account_type),
    CONSTRAINT ck_account_type CHECK (account_type IN ('debit', 'credit', 'undefined')),
    CONSTRAINT ck_account_type_lowercase CHECK (account_type = lower(account_type))
);

CREATE OR REPLACE FUNCTION fn_update_timestamp_account() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_update_timestamp_account ON t_account;
CREATE TRIGGER tr_update_timestamp_account
    BEFORE UPDATE
    ON t_account
    FOR EACH ROW
EXECUTE PROCEDURE fn_update_timestamp_account();

CREATE OR REPLACE FUNCTION fn_insert_timestamp_account() RETURNS TRIGGER AS
$$
BEGIN
    NEW.date_added := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_insert_timestamp_account ON t_account;
CREATE TRIGGER tr_insert_timestamp_account
    BEFORE INSERT
    ON t_account
    FOR EACH ROW
EXECUTE PROCEDURE fn_insert_timestamp_account();

--------------
-- Category --
--------------
CREATE TABLE IF NOT EXISTS t_category
(
    category_id   BIGSERIAL PRIMARY KEY,
    category      TEXT UNIQUE NOT NULL,
    active_status BOOLEAN     NOT NULL DEFAULT TRUE,
    date_updated  TIMESTAMP   NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added    TIMESTAMP   NOT NULL DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT ck_lowercase_category CHECK (category = lower(category))
);

CREATE OR REPLACE FUNCTION fn_insert_timestamp_category() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_added := CURRENT_TIMESTAMP;
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_insert_timestamp_category ON t_category;
CREATE TRIGGER tr_insert_timestamp_category
    BEFORE INSERT
    ON t_category
    FOR EACH ROW
EXECUTE PROCEDURE fn_insert_timestamp_category();

CREATE OR REPLACE FUNCTION fn_update_timestamp_category() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_update_timestamp_category ON t_category;
CREATE TRIGGER tr_update_timestamp_category
    BEFORE UPDATE
    ON t_category
    FOR EACH ROW
EXECUTE PROCEDURE fn_update_timestamp_category();

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
CREATE TABLE IF NOT EXISTS t_transaction
(
    transaction_id     BIGSERIAL PRIMARY KEY,
    account_id         BIGINT         NOT NULL,
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
    CONSTRAINT transaction_constraint UNIQUE (account_name_owner, transaction_date, description, category, amount, notes),
    CONSTRAINT t_transaction_description_lowercase_ck CHECK (description = lower(description)),
    CONSTRAINT t_transaction_category_lowercase_ck CHECK (category = lower(category)),
    CONSTRAINT t_transaction_notes_lowercase_ck CHECK (notes = lower(notes)),
    --CONSTRAINT fk_category_id_transaction_id FOREIGN KEY(transaction_id) REFERENCES t_transaction_categories(category_id, transaction_id) ON DELETE CASCADE,
    CONSTRAINT ck_transaction_state CHECK (transaction_state IN ('outstanding', 'future', 'cleared', 'undefined')),
    CONSTRAINT ck_account_type CHECK (account_type IN ('debit', 'credit', 'undefined')),
    CONSTRAINT fk_account_id_account_name_owner FOREIGN KEY (account_id, account_name_owner, account_type) REFERENCES t_account (account_id, account_name_owner, account_type) ON DELETE CASCADE,
    CONSTRAINT fk_category FOREIGN KEY (category) REFERENCES t_category (category) ON DELETE CASCADE
);

CREATE OR REPLACE FUNCTION fn_insert_timestamp_transaction() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_added := CURRENT_TIMESTAMP;
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_insert_timestamp_transaction ON t_transaction;
CREATE TRIGGER tr_insert_timestamp_transaction
    BEFORE INSERT
    ON t_transaction
    FOR EACH ROW
EXECUTE PROCEDURE fn_insert_timestamp_transaction();

CREATE OR REPLACE FUNCTION fn_update_timestamp_transaction() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_update_timestamp_transaction ON t_transaction;
CREATE TRIGGER tr_update_timestamp_transaction
    BEFORE UPDATE
    ON t_transaction
    FOR EACH ROW
EXECUTE PROCEDURE fn_update_timestamp_transaction();

-------------
-- Payment --
-------------
CREATE TABLE IF NOT EXISTS t_payment
(
    payment_id         BIGSERIAL PRIMARY KEY,
    account_name_owner TEXT           NOT NULL,
    transaction_date   DATE           NOT NULL,
    amount             DECIMAL(12, 2) NOT NULL DEFAULT 0.0,
    guid_source        TEXT           NOT NULL,
    guid_destination   TEXT           NOT NULL,
    date_updated       TIMESTAMP      NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added         TIMESTAMP      NOT NULL DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT payment_constraint UNIQUE (account_name_owner, transaction_date, amount),
    CONSTRAINT fk_guid_source FOREIGN KEY (guid_source) REFERENCES t_transaction (guid),
    CONSTRAINT fk_guid_destination FOREIGN KEY (guid_destination) REFERENCES t_transaction (guid)
);

CREATE OR REPLACE FUNCTION fn_insert_timestamp_payment() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_added := CURRENT_TIMESTAMP;
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_insert_timestamp_payment ON t_payment;
CREATE TRIGGER tr_insert_timestamp_payment
    BEFORE INSERT
    ON t_payment
    FOR EACH ROW
EXECUTE PROCEDURE fn_insert_timestamp_payment();

CREATE OR REPLACE FUNCTION fn_update_timestamp_payment() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_update_timestamp_payment ON t_payment;
CREATE TRIGGER tr_update_timestamp_payment
    BEFORE UPDATE
    ON t_payment
    FOR EACH ROW
EXECUTE PROCEDURE fn_update_timestamp_payment();


-------------
-- Parm --
-------------
CREATE TABLE IF NOT EXISTS t_parm
(
    parm_id      BIGSERIAL PRIMARY KEY,
    parm_name    TEXT   UNIQUE   NOT NULL,
    parm_value   TEXT      NOT NULL,
    date_updated TIMESTAMP NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added   TIMESTAMP NOT NULL DEFAULT TO_TIMESTAMP(0)
);

CREATE OR REPLACE FUNCTION fn_insert_timestamp_parm() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_added := CURRENT_TIMESTAMP;
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_insert_timestamp_parm ON t_parm;
CREATE TRIGGER tr_insert_timestamp_parm
    BEFORE INSERT
    ON t_parm
    FOR EACH ROW
EXECUTE PROCEDURE fn_insert_timestamp_parm();

CREATE OR REPLACE FUNCTION fn_update_timestamp_parm() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_update_timestamp_parm ON t_parm;
CREATE TRIGGER tr_update_timestamp_parm
    BEFORE UPDATE
    ON t_parm
    FOR EACH ROW
EXECUTE PROCEDURE fn_update_timestamp_parm();

--insert into t_parm(parm_name, parm_value) VALUES('payment_account', '');

COMMIT;
-- check for locks
-- SELECT pid, usename, pg_blocking_pids(pid) as blocked_by, query as blocked_query from pg_stat_activity where cardinality(pg_blocking_pids(pid)) > 0;
