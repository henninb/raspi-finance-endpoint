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
    totals             NUMERIC(8, 2)        DEFAULT 0.00,
    totals_balanced    NUMERIC(8, 2)        DEFAULT 0.00,
    date_closed        TIMESTAMP            DEFAULT TO_TIMESTAMP(0),
    date_updated       TIMESTAMP   NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added         TIMESTAMP   NOT NULL DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT unique_account_name_owner_account_id UNIQUE (account_id, account_name_owner, account_type),
    CONSTRAINT unique_account_name_owner_account_type UNIQUE (account_name_owner, account_type),
    CONSTRAINT ck_account_type CHECK (account_type IN ('debit', 'credit', 'undefined')),
    CONSTRAINT ck_account_type_lowercase CHECK (account_type = lower(account_type))
);

CREATE OR REPLACE FUNCTION fn_update_account() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_update_account ON t_account;
CREATE TRIGGER tr_update_account
    BEFORE UPDATE
    ON t_account
    FOR EACH ROW
EXECUTE PROCEDURE fn_update_account();

CREATE OR REPLACE FUNCTION fn_insert_account() RETURNS TRIGGER AS
$$
BEGIN
    NEW.active_status := true;
    NEW.date_updated := CURRENT_TIMESTAMP;
    NEW.date_added := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_insert_account ON t_account;
CREATE TRIGGER tr_insert_account
    BEFORE INSERT
    ON t_account
    FOR EACH ROW
EXECUTE PROCEDURE fn_insert_account();

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

CREATE OR REPLACE FUNCTION fn_insert_category() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.active_status := true;
    NEW.date_added := CURRENT_TIMESTAMP;
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_insert_category ON t_category;
CREATE TRIGGER tr_insert_category
    BEFORE INSERT
    ON t_category
    FOR EACH ROW
EXECUTE PROCEDURE fn_insert_category();

CREATE OR REPLACE FUNCTION fn_update_category() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_update_category ON t_category;
CREATE TRIGGER tr_update_category
    BEFORE UPDATE
    ON t_category
    FOR EACH ROW
EXECUTE PROCEDURE fn_update_category();

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

-------------------
-- ReceiptImage  --
-------------------
CREATE TABLE IF NOT EXISTS t_receipt_image
(
    receipt_image_id BIGSERIAL PRIMARY KEY,
    transaction_id   BIGINT    NOT NULL,
    jpg_image        BYTEA     NOT NULL,                         -- ADD the not NULL constraint
    active_status    BOOLEAN   NOT NULL DEFAULT TRUE,
    date_updated     TIMESTAMP NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added       TIMESTAMP NOT NULL DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT ck_jpg_size CHECK (length(jpg_image) <= 1048576), -- 1024 kb file size limit
    --646174613a696d6167652f706e673b626173653634 = data:image/png;base64
    --646174613a696d6167652f6a7065673b626173653634 = data:image/jpeg;base64
    --CONSTRAINT ck_image_type_png CHECK(left(encode(receipt_image,'hex'),42) = '646174613a696d6167652f706e673b626173653634'),
    CONSTRAINT ck_image_type_jpg CHECK (left(encode(jpg_image, 'hex'), 44) =
                                        '646174613a696d6167652f6a7065673b626173653634'),
    CONSTRAINT fk_transaction FOREIGN KEY (transaction_id) REFERENCES t_transaction (transaction_id) ON DELETE CASCADE
);
-- example
-- ALTER TABLE t_receipt_image ADD COLUMN date_updated     TIMESTAMP NOT NULL DEFAULT TO_TIMESTAMP(0);
-- ALTER TABLE t_receipt_image ADD CONSTRAINT ck_image_size CHECK(length(receipt_image) <= 1024);
-- select receipt_image_id, transaction_id, length(receipt_image)/1048576.0, left(encode(receipt_image,'hex'),100) from t_receipt_image;

CREATE OR REPLACE FUNCTION fn_insert_receipt_image() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.active_status := true;
    NEW.date_added := CURRENT_TIMESTAMP;
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_insert_receipt_image ON t_receipt_image;
CREATE TRIGGER tr_insert_receipt_image
    BEFORE INSERT
    ON t_receipt_image
    FOR EACH ROW
EXECUTE PROCEDURE fn_insert_receipt_image();

CREATE OR REPLACE FUNCTION fn_update_receipt_image() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_update_receipt_image ON t_receipt_image;
CREATE TRIGGER tr_update_receipt_image
    BEFORE UPDATE
    ON t_receipt_image
    FOR EACH ROW
EXECUTE PROCEDURE fn_update_receipt_image();

-----------------
-- Transaction --
-----------------
--CREATE TYPE transaction_state_enum AS ENUM ('outstanding','future','cleared', 'undefined');
--CREATE TYPE account_type_enum AS ENUM ('credit','debit', 'undefined');
CREATE TABLE IF NOT EXISTS t_transaction
(
    transaction_id     BIGSERIAL PRIMARY KEY,
    account_id         BIGINT        NOT NULL,
    account_type       TEXT          NOT NULL DEFAULT 'undefined',
    account_name_owner TEXT          NOT NULL,
    guid               TEXT          NOT NULL UNIQUE,
    transaction_date   DATE          NOT NULL,
    description        TEXT          NOT NULL,
    category           TEXT          NOT NULL DEFAULT '',
    amount             NUMERIC(8, 2) NOT NULL DEFAULT 0.00,
    transaction_state  TEXT          NOT NULL DEFAULT 'undefined',
    reoccurring        BOOLEAN       NOT NULL DEFAULT FALSE,
    reoccurring_type   TEXT          NULL     DEFAULT 'undefined',
    active_status      BOOLEAN       NOT NULL DEFAULT TRUE,
    notes              TEXT          NOT NULL DEFAULT '',
    receipt_image_id   BIGINT        NULL,
    date_updated       TIMESTAMP     NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added         TIMESTAMP     NOT NULL DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT transaction_constraint UNIQUE (account_name_owner, transaction_date, description, category, amount,
                                              notes),
    CONSTRAINT t_transaction_description_lowercase_ck CHECK (description = lower(description)),
    CONSTRAINT t_transaction_category_lowercase_ck CHECK (category = lower(category)),
    CONSTRAINT t_transaction_notes_lowercase_ck CHECK (notes = lower(notes)),
    --CONSTRAINT fk_category_id_transaction_id FOREIGN KEY(transaction_id) REFERENCES t_transaction_categories(category_id, transaction_id) ON DELETE CASCADE,
    CONSTRAINT ck_transaction_state CHECK (transaction_state IN ('outstanding', 'future', 'cleared', 'undefined')),
    CONSTRAINT ck_account_type CHECK (account_type IN ('debit', 'credit', 'undefined')),
    CONSTRAINT ck_reoccurring_type CHECK (reoccurring_type IN
                                          ('annually', 'bi-annually', 'every_two_weeks', 'monthly', 'undefined')),
    CONSTRAINT fk_account_id_account_name_owner FOREIGN KEY (account_id, account_name_owner, account_type) REFERENCES t_account (account_id, account_name_owner, account_type) ON DELETE CASCADE,
    CONSTRAINT fk_receipt_image FOREIGN KEY (receipt_image_id) REFERENCES t_receipt_image (receipt_image_id) ON DELETE CASCADE,
    CONSTRAINT fk_category FOREIGN KEY (category) REFERENCES t_category (category) ON DELETE CASCADE
);

-- example
-- ALTER TABLE t_transaction DROP CONSTRAINT IF EXISTS fk_receipt_image;
-- ALTER TABLE t_transaction ADD CONSTRAINT ck_reoccurring_type CHECK (reoccurring_type IN ('annually', 'bi-annually', 'every_two_weeks', 'monthly', 'undefined'));
-- ALTER TABLE t_transaction ADD COLUMN reoccurring_type TEXT NULL DEFAULT 'undefined';
-- ALTER TABLE t_transaction DROP COLUMN receipt_image_id;

CREATE OR REPLACE FUNCTION fn_insert_transaction() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.reoccurring_type = 'undefined';
    NEW.active_status := true;
    NEW.date_added := CURRENT_TIMESTAMP;
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_insert_transaction ON t_transaction;
CREATE TRIGGER tr_insert_transaction
    BEFORE INSERT
    ON t_transaction
    FOR EACH ROW
EXECUTE PROCEDURE fn_insert_transaction();

CREATE OR REPLACE FUNCTION fn_update_transaction() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_update_transaction ON t_transaction;
CREATE TRIGGER tr_update_transaction
    BEFORE UPDATE
    ON t_transaction
    FOR EACH ROW
EXECUTE PROCEDURE fn_update_transaction();

-------------
-- Payment --
-------------
CREATE TABLE IF NOT EXISTS t_payment
(
    payment_id         BIGSERIAL PRIMARY KEY,
    account_name_owner TEXT          NOT NULL,
    transaction_date   DATE          NOT NULL,
    amount             NUMERIC(8, 2) NOT NULL DEFAULT 0.00,
    guid_source        TEXT          NOT NULL,
    guid_destination   TEXT          NOT NULL,
    --TODO: bh 11/11/2020 - need to add this field
    --active_status      BOOLEAN        NOT NULL DEFAULT TRUE,
    date_updated       TIMESTAMP     NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added         TIMESTAMP     NOT NULL DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT payment_constraint UNIQUE (account_name_owner, transaction_date, amount),
    CONSTRAINT fk_guid_source FOREIGN KEY (guid_source) REFERENCES t_transaction (guid),
    CONSTRAINT fk_guid_destination FOREIGN KEY (guid_destination) REFERENCES t_transaction (guid)
);

CREATE OR REPLACE FUNCTION fn_insert_payment() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    --TODO: bh 11/11/2020 - need to add
    --NEW.active_status := true;
    NEW.date_added := CURRENT_TIMESTAMP;
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_insert_payment ON t_payment;
CREATE TRIGGER tr_insert_payment
    BEFORE INSERT
    ON t_payment
    FOR EACH ROW
EXECUTE PROCEDURE fn_insert_payment();

CREATE OR REPLACE FUNCTION fn_update_payment() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_update_payment ON t_payment;
CREATE TRIGGER tr_update_payment
    BEFORE UPDATE
    ON t_payment
    FOR EACH ROW
EXECUTE PROCEDURE fn_update_payment();

-------------
-- Parm --
-------------
CREATE TABLE IF NOT EXISTS t_parm
(
    parm_id       BIGSERIAL PRIMARY KEY,
    parm_name     TEXT UNIQUE NOT NULL,
    parm_value    TEXT        NOT NULL,
    active_status BOOLEAN     NOT NULL DEFAULT TRUE,
    date_updated  TIMESTAMP   NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added    TIMESTAMP   NOT NULL DEFAULT TO_TIMESTAMP(0)
);

-- example
-- ALTER TABLE t_parm ADD COLUMN active_status BOOLEAN NOT NULL DEFAULT TRUE;
-- insert into t_parm(parm_name, parm_value) VALUES('payment_account', '');

CREATE OR REPLACE FUNCTION fn_insert_parm() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.active_status := true;
    NEW.date_added := CURRENT_TIMESTAMP;
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_insert_parm ON t_parm;
CREATE TRIGGER tr_insert_parm
    BEFORE INSERT
    ON t_parm
    FOR EACH ROW
EXECUTE PROCEDURE fn_insert_parm();

CREATE OR REPLACE FUNCTION fn_update_parm() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_update_parm ON t_parm;
CREATE TRIGGER tr_update_parm
    BEFORE UPDATE
    ON t_parm
    FOR EACH ROW
EXECUTE PROCEDURE fn_update_parm();

-----------------
-- description --
-----------------
CREATE TABLE IF NOT EXISTS t_description
(
    description_id BIGSERIAL PRIMARY KEY,
    description    TEXT UNIQUE NOT NULL,
    active_status  BOOLEAN     NOT NULL DEFAULT TRUE,
    date_updated   TIMESTAMP   NOT NULL DEFAULT TO_TIMESTAMP(0),
    date_added     TIMESTAMP   NOT NULL DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT t_description_description_lowercase_ck CHECK (description = lower(description))
);

--ALTER TABLE t_description ADD COLUMN active_status      BOOLEAN        NOT NULL DEFAULT TRUE;

CREATE OR REPLACE FUNCTION fn_insert_description() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.active_status := true;
    NEW.date_added := CURRENT_TIMESTAMP;
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_insert_description ON t_description;
CREATE TRIGGER tr_insert_description
    BEFORE INSERT
    ON t_description
    FOR EACH ROW
EXECUTE PROCEDURE fn_insert_description();

CREATE OR REPLACE FUNCTION fn_update_description() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    NEW.date_updated := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_update_description ON t_description;
CREATE TRIGGER tr_update_description
    BEFORE UPDATE
    ON t_description
    FOR EACH ROW
EXECUTE PROCEDURE fn_update_description();

COMMIT;

-- check for locks
-- SELECT pid, usename, pg_blocking_pids(pid) as blocked_by, query as blocked_query from pg_stat_activity where cardinality(pg_blocking_pids(pid)) > 0;
