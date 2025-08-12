-- H2 Database Schema for Functional Tests

CREATE SCHEMA IF NOT EXISTS func;

-------------
-- Account --
-------------
CREATE TABLE IF NOT EXISTS func.t_account
(
    account_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_name_owner TEXT UNIQUE                           NOT NULL,
    account_name       TEXT                                  NULL,     -- NULL for now
    account_owner      TEXT                                  NULL,     -- NULL for now
    account_type       TEXT          DEFAULT 'unknown'       NOT NULL,
    active_status      BOOLEAN       DEFAULT TRUE            NOT NULL,
    payment_required   BOOLEAN                               NULL DEFAULT TRUE,
    moniker            TEXT          DEFAULT '0000'          NOT NULL,
    future             NUMERIC(8, 2) DEFAULT 0.00            NULL,
    outstanding        NUMERIC(8, 2) DEFAULT 0.00            NULL,
    cleared            NUMERIC(8, 2) DEFAULT 0.00            NULL,
    date_closed        TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    validation_date    TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_updated       TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added         TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT unique_account_name_owner_account_id UNIQUE (account_id, account_name_owner, account_type),
    CONSTRAINT unique_account_name_owner_account_type UNIQUE (account_name_owner, account_type),
    CONSTRAINT ck_account_type CHECK (account_type IN ('debit', 'credit', 'undefined')),
    CONSTRAINT ck_account_type_lowercase CHECK (account_type = lower(account_type))
);

-- ALTER TABLE t_account ADD COLUMN payment_required   BOOLEAN     NULL     DEFAULT TRUE;

----------------------------
-- Validation Amount Date --
----------------------------
CREATE TABLE IF NOT EXISTS func.t_validation_amount(
    validation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id         BIGINT                                NOT NULL,
    validation_date         TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    transaction_state  TEXT          DEFAULT 'undefined'     NOT NULL,
    amount             NUMERIC(8, 2) DEFAULT 0.00            NOT NULL,
    active_status      BOOLEAN       DEFAULT TRUE            NOT NULL,
    date_updated  TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added    TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT ck_validation_transaction_state CHECK (transaction_state IN ('outstanding', 'future', 'cleared', 'undefined')),
    CONSTRAINT fk_account_id FOREIGN KEY (account_id) REFERENCES func.t_account (account_id) ON DELETE CASCADE
);

--------------
-- User     --
--------------
CREATE TABLE IF NOT EXISTS func.t_user
(
    user_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      TEXT UNIQUE                       NOT NULL,
    password      TEXT                              NOT NULL,
    active_status BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated  TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added    TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT ck_lowercase_username CHECK (username = lower(username))
);

--------------
-- Role     --
--------------
CREATE TABLE IF NOT EXISTS func.t_role
(
    role_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    role          TEXT UNIQUE                       NOT NULL,
    active_status BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated  TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added    TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT ck_lowercase_role CHECK (role = lower(role))
);

--------------
-- Category --
--------------
CREATE TABLE IF NOT EXISTS func.t_category
(
    category_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name      TEXT UNIQUE                       NOT NULL,
    active_status BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated  TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added    TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT ck_lowercase_category CHECK (category_name = lower(category_name))
);

---------------------------
-- TransactionCategories --
---------------------------
CREATE TABLE IF NOT EXISTS func.t_transaction_categories
(
    category_id    BIGINT                            NOT NULL,
    transaction_id BIGINT                            NOT NULL,
    date_updated   TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added     TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    PRIMARY KEY (category_id, transaction_id)
);

-------------------
-- ReceiptImage  --
-------------------
CREATE TABLE IF NOT EXISTS func.t_receipt_image
(
    receipt_image_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id    BIGINT                            NOT NULL,
    image             BLOB                             NOT NULL,
    thumbnail         BLOB                             NOT NULL,
    image_format_type TEXT      DEFAULT 'undefined'     NOT NULL,
    active_status     BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated      TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added        TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT ck_image_size CHECK (length(image) <= 1048576), -- 1024 kb file size limit
    CONSTRAINT ck_image_type CHECK (image_format_type IN ('jpeg', 'png', 'undefined'))
);

-- alter table t_receipt_image rename column jpg_image to image;
-- alter table t_receipt_image alter column thumbnail set not null;
-- alter table t_receipt_image alter column image_format_type set not null;
-- ALTER TABLE t_receipt_image DROP CONSTRAINT ck_image_type_jpg;
-- ALTER TABLE t_receipt_image ADD COLUMN date_updated     TIMESTAMP NOT NULL DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S');
-- ALTER TABLE t_receipt_image ADD CONSTRAINT ck_image_size CHECK(length(image) <= 1_048_576);
-- select receipt_image_id, transaction_id, length(receipt_image)/1048576.0, left(encode(receipt_image,'hex'),100) from t_receipt_image;

-----------------
-- Transaction --
-----------------
--CREATE TYPE transaction_state_enum AS ENUM ('outstanding','future','cleared', 'undefined');
--CREATE TYPE account_type_enum AS ENUM ('credit','debit', 'undefined');
CREATE TABLE IF NOT EXISTS func.t_transaction
(
    transaction_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id         BIGINT                                NOT NULL,
    account_type       TEXT          DEFAULT 'undefined'     NOT NULL,
    transaction_type   TEXT          DEFAULT 'undefined'     NOT NULL,
    account_name_owner TEXT                                  NOT NULL,
    guid               TEXT UNIQUE                           NOT NULL,
    transaction_date   DATE                                  NOT NULL,
    due_date           DATE                                  NULL,
    description        TEXT                                  NOT NULL,
    category           TEXT          DEFAULT ''              NOT NULL,
    amount             NUMERIC(8, 2) DEFAULT 0.00            NOT NULL,
    transaction_state  TEXT          DEFAULT 'undefined'     NOT NULL,
    reoccurring_type   TEXT          DEFAULT 'undefined'     NULL,
    active_status      BOOLEAN       DEFAULT TRUE            NOT NULL,
    notes              TEXT          DEFAULT ''              NOT NULL,
    receipt_image_id   BIGINT                                NULL,
    date_updated       TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added         TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT transaction_constraint UNIQUE (account_name_owner, transaction_date, description, category, amount,
                                              notes),
    CONSTRAINT t_transaction_description_lowercase_ck CHECK (description = lower(description)),
    CONSTRAINT t_transaction_category_lowercase_ck CHECK (category = lower(category)),
    CONSTRAINT t_transaction_notes_lowercase_ck CHECK (notes = lower(notes)),
    CONSTRAINT ck_t_transaction_state CHECK (transaction_state IN ('outstanding', 'future', 'cleared', 'undefined')),
    CONSTRAINT ck_t_transaction_account_type CHECK (account_type IN ('debit', 'credit', 'undefined')),
    CONSTRAINT ck_transaction_type CHECK (transaction_type IN ('expense', 'income', 'transfer', 'undefined')),
    CONSTRAINT ck_reoccurring_type CHECK (reoccurring_type IN
                                          ('annually', 'biannually', 'fortnightly', 'monthly', 'quarterly', 'onetime',
                                           'undefined')),
    CONSTRAINT fk_account_id_account_name_owner FOREIGN KEY (account_id, account_name_owner, account_type) REFERENCES func.t_account (account_id, account_name_owner, account_type) ON DELETE CASCADE,
    CONSTRAINT fk_receipt_image FOREIGN KEY (receipt_image_id) REFERENCES func.t_receipt_image (receipt_image_id) ON DELETE CASCADE,
    CONSTRAINT fk_category FOREIGN KEY (category) REFERENCES func.t_category (category_name) ON DELETE CASCADE
);

-- Required to happen after the t_transaction table is created
ALTER TABLE func.t_receipt_image
    DROP CONSTRAINT IF EXISTS fk_transaction;
ALTER TABLE func.t_receipt_image
    ADD CONSTRAINT fk_transaction FOREIGN KEY (transaction_id) REFERENCES func.t_transaction (transaction_id) ON DELETE CASCADE;

-- ALTER TABLE t_transaction DROP CONSTRAINT IF EXISTS ck_reoccurring_type;
-- ALTER TABLE t_transaction DROP CONSTRAINT IF EXISTS fk_receipt_image;
-- ALTER TABLE t_transaction ADD CONSTRAINT ck_reoccurring_type CHECK (reoccurring_type IN ('annually', 'bi-annually', 'fortnightly', 'monthly', 'quarterly', 'undefined'));
-- ALTER TABLE t_transaction ADD COLUMN reoccurring_type TEXT NULL DEFAULT 'undefined';
-- ALTER TABLE t_transaction DROP COLUMN receipt_image_id;

-------------
-- Payment --
-------------
CREATE TABLE IF NOT EXISTS func.t_payment
(
    payment_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_name_owner TEXT                                  NOT NULL,
    source_account     TEXT                                  NOT NULL,
    destination_account TEXT                                 NOT NULL,
    transaction_date   DATE                                  NOT NULL,
    amount             NUMERIC(8, 2) DEFAULT 0.00            NOT NULL,
    guid_source        TEXT                                  NOT NULL,
    guid_destination   TEXT                                  NOT NULL,
    active_status      BOOLEAN       DEFAULT TRUE            NOT NULL,
    date_updated       TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added         TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT payment_constraint UNIQUE (account_name_owner, transaction_date, amount),
    CONSTRAINT fk_guid_source FOREIGN KEY (guid_source) REFERENCES func.t_transaction (guid) ON DELETE CASCADE,
    CONSTRAINT fk_guid_destination FOREIGN KEY (guid_destination) REFERENCES func.t_transaction (guid) ON DELETE CASCADE
);

-- ALTER table t_payment drop constraint fk_guid_source, add CONSTRAINT fk_guid_source FOREIGN KEY (guid_source) REFERENCES func.t_transaction (guid) ON DELETE CASCADE;
-- ALTER table t_payment drop constraint fk_guid_destination, add CONSTRAINT fk_guid_destination FOREIGN KEY (guid_destination) REFERENCES func.t_transaction (guid) ON DELETE CASCADE;

-------------
-- parameter    --
-------------
CREATE TABLE IF NOT EXISTS func.t_parameter
(
    parameter_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    parameter_name     TEXT UNIQUE                       NOT NULL,
    parameter_value    TEXT                              NOT NULL,
    active_status BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated  TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added    TIMESTAMP                         NOT NULL DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S')
);

-- ALTER TABLE t_parameter ADD COLUMN active_status BOOLEAN NOT NULL DEFAULT TRUE;
-- INSERT into t_parameter(parameter_name, parameter_value) VALUES('payment_account', '');

-----------------
-- description --
-----------------
CREATE TABLE IF NOT EXISTS func.t_description
(
    description_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description_name    TEXT UNIQUE                       NOT NULL,
    active_status  BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated   TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added     TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT t_description_description_lowercase_ck CHECK (description_name = lower(description_name))
);

-- ALTER TABLE t_description ADD COLUMN active_status      BOOLEAN        NOT NULL DEFAULT TRUE;

-- H2 manages sequences automatically for AUTO_INCREMENT columns
-- PostgreSQL functions and triggers removed - not needed for H2 functional tests
