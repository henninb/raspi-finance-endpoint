-- drop table T_TRANSACTION_CATEGORIES cascade constraints;
-- drop table T_PAYMENT cascade constraints;
-- drop table T_TRANSACTION cascade constraints;
-- drop table T_ACCOUNT cascade constraints;
-- drop table T_CATEGORY cascade constraints;
-- drop table T_RECEIPT_IMAGE cascade constraints;
-- drop table T_PARM cascade constraints;
-- drop table T_DESCRIPTION cascade constraints;

alter
session
set "_ORACLE_SCRIPT" = TRUE;
create
profile umlimited_attempts limit failed_login_attempts unlimited;

create
    user henninb identified by monday1;
grant connect, resource, create any context to henninb;
GRANT CONNECT, RESOURCE, DBA TO henninb;
-- GRANT sysdba to henninb;
alter
    user henninb profile umlimited_attempts;
alter
    user system profile umlimited_attempts;
alter
    user henninb quota unlimited on users;
alter
    user henninb quota unlimited on system;


--ALTER USER system ACCOUNT UNLOCK

--------------
-- Account --
--------------
CREATE TABLE t_account
(
    account_id         NUMBER GENERATED always AS IDENTITY PRIMARY KEY,
    account_name_owner VARCHAR(30) UNIQUE                NOT NULL,
    account_name       VARCHAR(30), -- NULL for now
    account_owner      VARCHAR(30), -- NULL for now
    account_type       VARCHAR(20)   DEFAULT 'undefined' NOT NULL,
    active_status      NUMBER(1)     DEFAULT '1'         NOT NULL,
    payment_required   NUMBER(1)     DEFAULT '1'         NULL,
    moniker            VARCHAR(10)   DEFAULT '0000'      NOT NULL,
    future             DECIMAL(8, 2) DEFAULT 0.00,
    outstanding        DECIMAL(8, 2) DEFAULT 0.00,
    cleared            DECIMAL(8, 2) DEFAULT 0.00,
    date_closed        TIMESTAMP,
    date_updated       TIMESTAMP                         NOT NULL,
    date_added         TIMESTAMP                         NOT NULL,
    CONSTRAINT unique_account_name_owner_account_id UNIQUE (account_id, account_name_owner, account_type),
    CONSTRAINT unique_account_name_owner_account_type UNIQUE (account_name_owner, account_type),
    CONSTRAINT ck_account_type CHECK (account_type IN ('debit', 'credit', 'undefined')),
    CONSTRAINT ck_account_type_lowercase CHECK (account_type = lower(account_type))
);

--------------
-- Category --
--------------
CREATE TABLE t_category
(
    category_id   NUMBER GENERATED always AS IDENTITY PRIMARY KEY,
    category      VARCHAR(30) UNIQUE  NOT NULL,
    active_status NUMBER(1) DEFAULT 1 NOT NULL,
    date_updated  TIMESTAMP           NOT NULL,
    date_added    TIMESTAMP           NOT NULL,
    CONSTRAINT ck_lowercase_category CHECK (category = lower(category))
);

---------------------------
-- TransactionCategories --
---------------------------
-- CREATE TABLE t_transaction_categories
-- (
--     category_id    NUMBER    NOT NULL,
--     transaction_id NUMBER    NOT NULL,
--     date_updated   TIMESTAMP NULL,
--     date_added     TIMESTAMP NULL,
--     PRIMARY KEY (category_id, transaction_id)
-- );

-------------------
-- ReceiptImage  --
-------------------
CREATE TABLE t_receipt_image
(
    receipt_image_id  NUMBER GENERATED always AS IDENTITY PRIMARY KEY,
    transaction_id    NUMBER                          NOT NULL,
    image             BLOB                            NOT NULL,
    thumbnail         BLOB                            NOT NULL,
    image_format_type VARCHAR(10) DEFAULT 'undefined' NOT NULL,
    active_status     NUMBER(1)   DEFAULT 1           NOT NULL,
    date_updated      TIMESTAMP                       NOT NULL,
    date_added        TIMESTAMP                       NOT NULL,
    CONSTRAINT ck_image_size CHECK (length(image) <= 1048576), -- 1024 kb file size limit
    CONSTRAINT ck_image_type CHECK (image_format_type IN ('jpeg', 'png', 'undefined'))
    --CONSTRAINT fk_transaction FOREIGN KEY (transaction_id) REFERENCES t_transaction (transaction_id) ON DELETE CASCADE
);

-----------------
-- Transaction --
-----------------
CREATE TABLE t_transaction
(
    transaction_id     NUMBER GENERATED always AS IDENTITY PRIMARY KEY,
    account_id         NUMBER                            NOT NULL,
    account_type       VARCHAR(20)   DEFAULT 'undefined' NOT NULL,
    account_name_owner VARCHAR(25)                       NOT NULL,
    guid               VARCHAR(40)                       NOT NULL UNIQUE,
    transaction_date   DATE                              NOT NULL,
    due_date           DATE                              NULL,
    description        VARCHAR(50)                       NOT NULL,
    category           VARCHAR(30)   DEFAULT ''          NOT NULL,
    amount             DECIMAL(8, 2) DEFAULT 0.0         NOT NULL,
    transaction_state  VARCHAR(30)                       NOT NULL,
    reoccurring_type   VARCHAR(30)   DEFAULT 'undefined' NULL,
    active_status      NUMBER(1)     DEFAULT 1           NOT NULL,
    notes              VARCHAR(100)  DEFAULT ''          NULL,
    receipt_image_id   NUMBER                            NULL,
    date_updated       TIMESTAMP                         NOT NULL,
    date_added         TIMESTAMP                         NOT NULL,
    CONSTRAINT transaction_constraint UNIQUE (account_name_owner, transaction_date, description, category, amount,
                                              notes),
    CONSTRAINT t_transaction_description_lowercase_ck CHECK (description = lower(description)),
    CONSTRAINT t_transaction_category_lowercase_ck CHECK (category = lower(category)),
    --CONSTRAINT t_transaction_notes_lowercase_ck CHECK (notes = lower(notes)),
    CONSTRAINT ck_transaction_state CHECK (transaction_state IN ('outstanding', 'future', 'cleared', 'undefined')),
    CONSTRAINT check_account_type CHECK (account_type IN ('debit', 'credit', 'undefined')),
    CONSTRAINT ck_reoccurring_type CHECK (reoccurring_type IN
                                          ('annually', 'biannually', 'fortnightly', 'monthly', 'quarterly','onetime', 'undefined')),
    CONSTRAINT fk_account_id_account_name_owner FOREIGN KEY (account_id, account_name_owner, account_type) REFERENCES t_account (account_id, account_name_owner, account_type) ON DELETE CASCADE,
    CONSTRAINT fk_receipt_image FOREIGN KEY (receipt_image_id) REFERENCES t_receipt_image (receipt_image_id) ON DELETE CASCADE,
    CONSTRAINT fk_category FOREIGN KEY (category) REFERENCES t_category (category) ON DELETE CASCADE
);


-------------
-- Payment --
-------------
CREATE TABLE t_payment
(
    payment_id         NUMBER GENERATED always AS IDENTITY PRIMARY KEY,
    account_name_owner VARCHAR(25)                NOT NULL,
    transaction_date   DATE                       NOT NULL,
    amount             NUMERIC(8, 2) DEFAULT 0.00 NOT NULL,
    guid_source        VARCHAR(40)                NOT NULL,
    guid_destination   VARCHAR(40)                NOT NULL,
    active_status      NUMBER(1)     DEFAULT 1    NOT NULL,
    date_updated       TIMESTAMP                  NOT NULL,
    date_added         TIMESTAMP                  NOT NULL,
    CONSTRAINT payment_constraint UNIQUE (account_name_owner, transaction_date, amount),
    CONSTRAINT fk_guid_source FOREIGN KEY (guid_source) REFERENCES t_transaction (guid) ON DELETE CASCADE,
    CONSTRAINT fk_guid_destination FOREIGN KEY (guid_destination) REFERENCES t_transaction (guid) ON DELETE CASCADE
);

----------
-- parameter --
----------
CREATE TABLE t_parameter
(
    parameter_id       NUMBER GENERATED always AS IDENTITY PRIMARY KEY,
    parameter_name     VARCHAR(30) UNIQUE  NOT NULL,
    parameter_value    VARCHAR(30)         NOT NULL,
    active_status NUMBER(1) DEFAULT 1 NOT NULL,
    date_updated  TIMESTAMP           NOT NULL,
    date_added    TIMESTAMP           NOT NULL
);
-----------------
-- description --
-----------------
CREATE TABLE t_description
(
    description_id NUMBER GENERATED always AS IDENTITY PRIMARY KEY,
    description    VARCHAR(50) UNIQUE  NOT NULL,
    active_status  NUMBER(1) DEFAULT 1 NOT NULL,
    date_updated   TIMESTAMP           NOT NULL,
    date_added     TIMESTAMP           NOT NULL
    -- CONSTRAINT t_description_description_lowercase_ck CHECK (description = lower(description))
);

-- t_account
CREATE
OR
REPLACE
TRIGGER tr_insert_account
    AFTER
INSERT
ON t_account
    FOR EACH ROW
BEGIN
dbms_output.put_line
        (
                'account_name_owner: ' || :new.ACCOUNT_NAME || ' account_type: ' || :new.ACCOUNT_TYPE
        );
END;


--select * from USER_TRIGGERS;
