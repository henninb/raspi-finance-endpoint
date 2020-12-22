-- drop table T_TRANSACTION_CATEGORIES cascade constraints;
-- drop table T_PAYMENT cascade constraints;
-- drop table T_TRANSACTION cascade constraints;
-- drop table T_ACCOUNT cascade constraints;
-- drop table T_CATEGORY cascade constraints;
-- drop table T_RECEIPT_IMAGE cascade constraints;
-- drop table T_PARM cascade constraints;
-- drop table T_DESCRIPTION cascade constraints;

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
    active_status      NUMBER(1)       DEFAULT '1'         NOT NULL,
    moniker            VARCHAR(10)   DEFAULT '0000'      NOT NULL,
    totals             DECIMAL(8, 2) DEFAULT 0.0,
    totals_balanced    DECIMAL(8, 2) DEFAULT 0.0,
    date_closed        TIMESTAMP,
    date_updated       TIMESTAMP                         NULL,
    date_added         TIMESTAMP                         NULL,
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
    category      VARCHAR(30) UNIQUE NOT NULL,
    active_status NUMBER(1) DEFAULT 1  NOT NULL,
    date_updated  TIMESTAMP          NULL,
    date_added    TIMESTAMP          NULL,
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
    receipt_image_id NUMBER GENERATED always AS IDENTITY PRIMARY KEY,
    transaction_id   NUMBER            NOT NULL,
    jpg_image        BLOB              NOT NULL,                -- ADD the not NULL constraint
    active_status    NUMBER(1) DEFAULT 1 NOT NULL,
    date_updated     TIMESTAMP         NULL,
    date_added       TIMESTAMP         NULL,
    CONSTRAINT ck_jpg_size CHECK (length(jpg_image) <= 1048576) -- 1024 kb file size limit
    --646174613a696d6167652f706e673b626173653634 = data:image/png;base64
    --646174613a696d6167652f6a7065673b626173653634 = data:image/jpeg;base64
    --CONSTRAINT ck_image_type_png CHECK(left(encode(receipt_image,'hex'),42) = '646174613a696d6167652f706e673b626173653634'),
    --CONSTRAINT ck_image_type_jpg CHECK (left(encode(jpg_image, 'hex'), 44) =
    --                                   '646174613a696d6167652f6a7065673b626173653634')
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
    description        VARCHAR(50)                       NOT NULL,
    category           VARCHAR(30)   DEFAULT ''          NOT NULL,
    amount             DECIMAL(8, 2) DEFAULT 0.0         NOT NULL,
    transaction_state  VARCHAR(30)                       NOT NULL,
    reoccurring        CHAR(1)       DEFAULT 1           NOT NULL,
    reoccurring_type   VARCHAR(30)   DEFAULT 'undefined' NULL,
    active_status      NUMBER(1)       DEFAULT 1           NOT NULL,
    notes              VARCHAR(100)  DEFAULT ''          NULL,
    receipt_image_id   NUMBER                            NULL,
    date_updated       TIMESTAMP                         NULL,
    date_added         TIMESTAMP                         NULL,
    CONSTRAINT transaction_constraint UNIQUE (account_name_owner, transaction_date, description, category, amount,
                                              notes),
    CONSTRAINT t_transaction_description_lowercase_ck CHECK (description = lower(description)),
    -- removed because of how oracle treats lower case
    CONSTRAINT t_transaction_category_lowercase_ck CHECK (category = lower(category)),
    --CONSTRAINT t_transaction_notes_lowercase_ck CHECK (notes = lower(notes)),
    --CONSTRAINT fk_category_id_transaction_id FOREIGN KEY(transaction_id) REFERENCES t_transaction_categories(category_id, transaction_id) ON DELETE CASCADE,
    CONSTRAINT ck_transaction_state CHECK (transaction_state IN ('outstanding', 'future', 'cleared', 'undefined')),
    CONSTRAINT check_account_type CHECK (account_type IN ('debit', 'credit', 'undefined')),
    CONSTRAINT ck_reoccurring_type CHECK (reoccurring_type IN
                                          ('annually', 'bi-annually', 'every_two_weeks', 'monthly', 'undefined')),
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
    active_status      NUMBER(1)       DEFAULT 1    NOT NULL,
    date_updated       TIMESTAMP                  NULL,
    date_added         TIMESTAMP                  NULL,
    CONSTRAINT payment_constraint UNIQUE (account_name_owner, transaction_date, amount),
    CONSTRAINT fk_guid_source FOREIGN KEY (guid_source) REFERENCES t_transaction (guid),
    CONSTRAINT fk_guid_destination FOREIGN KEY (guid_destination) REFERENCES t_transaction (guid)
);

-------------
-- Parm --
-------------
CREATE TABLE t_parm
(
    parm_id       NUMBER GENERATED always AS IDENTITY PRIMARY KEY,
    parm_name     VARCHAR(30) UNIQUE NOT NULL,
    parm_value    VARCHAR(30)        NOT NULL,
    active_status NUMBER(1) DEFAULT 1  NOT NULL,
    date_updated  TIMESTAMP          NULL,
    date_added    TIMESTAMP          NULL
);
-----------------
-- description --
-----------------
CREATE TABLE t_description
(
    description_id NUMBER GENERATED always AS IDENTITY PRIMARY KEY,
    description    VARCHAR(50) UNIQUE NOT NULL,
    active_status  NUMBER(1) DEFAULT 1  NOT NULL,
    date_updated   TIMESTAMP          NULL,
    date_added     TIMESTAMP          NULL
    -- CONSTRAINT t_description_description_lowercase_ck CHECK (description = lower(description))
);

-- -- t_account
-- CREATE OR REPLACE TRIGGER tr_insert_account
-- AFTER INSERT
--       ON t_account
--           FOR EACH ROW
-- BEGIN
-- UPDATE t_account(date_updated, date_added) VALUES(sysdate, sysdate);
-- END;
--
-- -- t_transaction_categories
-- CREATE OR REPLACE TRIGGER tr_insert_transaction_categories
-- AFTER INSERT
--       ON t_transaction_categories
--           FOR EACH ROW
-- BEGIN
-- UPDATE t_transaction_categories(date_updated, date_added) VALUES(sysdate, sysdate);
-- END;
--
-- -- t_receipt_image
-- CREATE OR REPLACE TRIGGER tr_insert_receipt_image
-- AFTER INSERT
--       ON t_receipt_image
--           FOR EACH ROW
-- BEGIN
-- UPDATE t_receipt_image(date_updated, date_added) VALUES(sysdate, sysdate);
-- END;
--
-- -- t_transaction
-- CREATE OR REPLACE TRIGGER tr_insert_transaction
-- AFTER INSERT
--       ON t_transaction
--           FOR EACH ROW
-- BEGIN
-- UPDATE t_transaction(date_updated, date_added) VALUES(sysdate, sysdate);
-- END;
--
-- -- t_payment
-- CREATE OR REPLACE TRIGGER tr_insert_payment
-- AFTER INSERT
--       ON t_payment
--           FOR EACH ROW
-- BEGIN
-- UPDATE t_payment(date_updated, date_added) VALUES(sysdate, sysdate);
-- END;
--
-- -- t_parm
-- CREATE OR REPLACE TRIGGER tr_insert_parm
-- AFTER INSERT
--       ON t_parm
--           FOR EACH ROW
-- BEGIN
-- UPDATE t_parm(date_updated, date_added) VALUES(sysdate, sysdate);
-- END;
--
-- -- t_description
-- CREATE OR REPLACE TRIGGER tr_insert_description
-- AFTER INSERT
--       ON t_description
--           FOR EACH ROW
-- BEGIN
-- UPDATE t_description(date_updated, date_added) VALUES(sysdate, sysdate);
-- END;

-- -- t_account
-- CREATE OR REPLACE TRIGGER tr_update_account
-- AFTER INSERT
--       ON t_account
--           FOR EACH ROW
-- BEGIN
-- UPDATE t_account(date_updated) VALUES(sysdate);
-- END;
--
-- -- t_transaction_categories
-- CREATE OR REPLACE TRIGGER tr_update_transaction_categories
-- AFTER INSERT
--       ON t_transaction_categories
--           FOR EACH ROW
-- BEGIN
-- UPDATE t_transaction_categories(date_updated) VALUES(sysdate);
-- END;
--
-- -- t_receipt_image
-- CREATE OR REPLACE TRIGGER tr_update_receipt_image
-- AFTER INSERT
--       ON t_receipt_image
--           FOR EACH ROW
-- BEGIN
-- UPDATE t_receipt_image(date_updated) VALUES(sysdate);
-- END;
--
-- -- t_transaction
-- CREATE OR REPLACE TRIGGER tr_update_transaction
-- AFTER INSERT
--       ON t_transaction
--           FOR EACH ROW
-- BEGIN
-- UPDATE t_transaction(date_updated) VALUES(sysdate);
-- END;
--
-- -- t_payment
-- CREATE OR REPLACE TRIGGER tr_update_payment
-- AFTER INSERT
--       ON t_payment
--           FOR EACH ROW
-- BEGIN
-- UPDATE t_payment(date_updated) VALUES(sysdate);
-- END;
--
-- -- t_parm
-- CREATE OR REPLACE TRIGGER tr_update_parm
-- AFTER INSERT
--       ON t_parm
--           FOR EACH ROW
-- BEGIN
-- UPDATE t_parm(date_updated) VALUES(sysdate);
-- END;
--
-- -- t_description
-- CREATE OR REPLACE TRIGGER tr_update_description
-- AFTER INSERT
--       ON t_description
--           FOR EACH ROW
-- BEGIN
-- UPDATE t_description(date_updated) VALUES(sysdate);
-- END;
