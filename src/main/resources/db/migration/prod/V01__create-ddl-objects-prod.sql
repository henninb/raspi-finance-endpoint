CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SET client_min_messages TO WARNING;

CREATE SCHEMA IF NOT EXISTS public;

-------------
-- Account --
-------------
CREATE TABLE IF NOT EXISTS public.t_account
(
    account_id         BIGSERIAL PRIMARY KEY,
    account_name_owner TEXT UNIQUE                           NOT NULL,
    account_name       TEXT                                  NULL,     -- NULL for now 6/30/2021
    account_owner      TEXT                                  NULL,     -- NULL for now 6/30/2021
    account_type       TEXT          DEFAULT 'unknown'       NOT NULL,
    active_status      BOOLEAN       DEFAULT TRUE            NOT NULL,
    payment_required   BOOLEAN                               NULL DEFAULT TRUE,
    moniker            TEXT          DEFAULT '0000'          NOT NULL,
    future             NUMERIC(8, 2) DEFAULT 0.00            NULL,
    outstanding        NUMERIC(8, 2) DEFAULT 0.00            NULL,
    cleared            NUMERIC(8, 2) DEFAULT 0.00            NULL,
    date_closed        TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL, -- TODO: should be null by default
    owner              TEXT                                  NULL,
    date_updated       TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added         TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT unique_account_name_owner_account_id UNIQUE (account_id, account_name_owner, account_type),
    CONSTRAINT unique_account_name_owner_account_type UNIQUE (account_name_owner, account_type),
    CONSTRAINT ck_account_type CHECK (account_type IN ('debit', 'credit', 'undefined')),
    CONSTRAINT ck_account_type_lowercase CHECK (account_type = lower(account_type))
);

-- ALTER TABLE public.t_account ADD COLUMN payment_required   BOOLEAN     NULL     DEFAULT TRUE;

----------------------------
-- Validation Amount Date --
----------------------------
CREATE TABLE IF NOT EXISTS public.t_validation_amount
(
    validation_id     BIGSERIAL PRIMARY KEY,
    account_id        BIGINT                                NOT NULL,
    validation_date   TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    transaction_state TEXT          DEFAULT 'undefined'     NOT NULL,
    amount            NUMERIC(8, 2) DEFAULT 0.00            NOT NULL,
    owner             TEXT                                  NULL,
    active_status     BOOLEAN       DEFAULT TRUE            NOT NULL,
    date_updated      TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added        TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT ck_transaction_state CHECK (transaction_state IN ('outstanding', 'future', 'cleared', 'undefined')),
    CONSTRAINT fk_account_id FOREIGN KEY (account_id) REFERENCES public.t_account (account_id) ON DELETE CASCADE
);

--------------
-- User     --
--------------
CREATE TABLE IF NOT EXISTS public.t_user
(
    user_id       BIGSERIAL PRIMARY KEY,
    username      TEXT UNIQUE                       NOT NULL,
    password      TEXT                              NOT NULL,
    active_status BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated  TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added    TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT ck_lowercase_username CHECK (username = lower(username))
);

--------------
-- Role     --
--------------
CREATE TABLE IF NOT EXISTS public.t_role
(
    role_id       BIGSERIAL PRIMARY KEY,
    role          TEXT UNIQUE                       NOT NULL,
    active_status BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated  TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added    TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT ck_lowercase_username CHECK (role = lower(role))
);

--------------
-- Category --
--------------
CREATE TABLE IF NOT EXISTS public.t_category
(
    category_id   BIGSERIAL PRIMARY KEY,
    category_name      TEXT UNIQUE                  NOT NULL,
    owner             TEXT                          NULL,
    active_status BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated  TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added    TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT ck_lowercase_category CHECK (category_name = lower(category_name))
);

-- ALTER TABLE public.t_category RENAME COLUMN category TO category_name;

---------------------------
-- TransactionCategories --
---------------------------
CREATE TABLE IF NOT EXISTS public.t_transaction_categories
(
    category_id    BIGINT                            NOT NULL,
    transaction_id BIGINT                            NOT NULL,
    owner             TEXT                           NULL,
    date_updated   TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added     TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    PRIMARY KEY (category_id, transaction_id)
);

-------------------
-- ReceiptImage  --
-------------------
CREATE TABLE IF NOT EXISTS public.t_receipt_image
(
    receipt_image_id  BIGSERIAL PRIMARY KEY,
    transaction_id    BIGINT                            NOT NULL,
    image             BYTEA                             NOT NULL,
    thumbnail         BYTEA                             NOT NULL,
    image_format_type TEXT      DEFAULT 'undefined'     NOT NULL,
    owner             TEXT                              NULL,
    active_status     BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated      TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added        TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT ck_image_size CHECK (length(image) <= 1048576), -- 1024 kb file size limit
    CONSTRAINT ck_image_type CHECK (image_format_type IN ('jpeg', 'png', 'undefined'))
);

-- ALTER TABLE public.t_receipt_image rename column jpg_image to image;
-- ALTER TABLE public.t_receipt_image alter column thumbnail set not null;
-- ALTER TABLE public.t_receipt_image alter column image_format_type set not null;
-- ALTER TABLE public.t_receipt_image DROP CONSTRAINT ck_image_type_jpg;
-- ALTER TABLE public.t_receipt_image ADD COLUMN date_updated     TIMESTAMP NOT NULL DEFAULT TO_TIMESTAMP(0);
-- ALTER TABLE public.t_receipt_image ADD CONSTRAINT ck_image_size CHECK(length(image) <= 1_048_576);
-- select receipt_image_id, transaction_id, length(receipt_image)/1048576.0, left(encode(receipt_image,'hex'),100) from t_receipt_image;

-----------------
-- Transaction --
-----------------
--CREATE TYPE transaction_state_enum AS ENUM ('outstanding','future','cleared', 'undefined');
--CREATE TYPE account_type_enum AS ENUM ('credit','debit', 'undefined');
CREATE TABLE IF NOT EXISTS public.t_transaction
(
    transaction_id     BIGSERIAL PRIMARY KEY,
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
    owner              TEXT                                  NULL,
    date_updated       TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added         TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT transaction_constraint UNIQUE (account_name_owner, transaction_date, description, category, amount,
                                              notes),
    CONSTRAINT t_transaction_description_lowercase_ck CHECK (description = lower(description)),
    CONSTRAINT t_transaction_category_lowercase_ck CHECK (category = lower(category)),
    CONSTRAINT t_transaction_notes_lowercase_ck CHECK (notes = lower(notes)),
    CONSTRAINT ck_transaction_state CHECK (transaction_state IN ('outstanding', 'future', 'cleared', 'undefined')),
    CONSTRAINT ck_account_type CHECK (account_type IN ('debit', 'credit', 'undefined')),
    CONSTRAINT ck_transaction_type CHECK (transaction_type IN ('expense', 'income', 'transfer', 'undefined')),
    CONSTRAINT ck_reoccurring_type CHECK (reoccurring_type IN
                                          ('annually', 'biannually', 'fortnightly', 'monthly', 'quarterly', 'onetime',
                                           'undefined')),
    CONSTRAINT fk_account_id_account_name_owner FOREIGN KEY (account_id, account_name_owner, account_type) REFERENCES public.t_account (account_id, account_name_owner, account_type) ON DELETE CASCADE,
    CONSTRAINT fk_receipt_image FOREIGN KEY (receipt_image_id) REFERENCES public.t_receipt_image (receipt_image_id) ON DELETE CASCADE,
    CONSTRAINT fk_category FOREIGN KEY (category) REFERENCES public.t_category (category_name) ON DELETE CASCADE
);

-- Required to happen after the t_transaction table is created
ALTER TABLE public.t_receipt_image
    DROP CONSTRAINT IF EXISTS fk_transaction;
ALTER TABLE public.t_receipt_image
    ADD CONSTRAINT fk_transaction FOREIGN KEY (transaction_id) REFERENCES public.t_transaction (transaction_id) ON DELETE CASCADE;

-- ALTER TABLE public.t_transaction DROP CONSTRAINT IF EXISTS ck_reoccurring_type;
-- ALTER TABLE public.t_transaction DROP CONSTRAINT IF EXISTS fk_receipt_image;
-- ALTER TABLE public.t_transaction ADD CONSTRAINT ck_reoccurring_type CHECK (reoccurring_type IN ('annually', 'biannually', 'fortnightly', 'monthly', 'quarterly', 'onetime', 'undefined'));
-- ALTER TABLE public.t_transaction ADD COLUMN transaction_type TEXT NULL DEFAULT 'undefined';
-- ALTER TABLE public.t_transaction ADD COLUMN reoccurring_type TEXT NULL DEFAULT 'undefined';
-- ALTER TABLE public.t_transaction DROP COLUMN reoccurring;

-------------
-- Payment --
-------------
CREATE TABLE IF NOT EXISTS public.t_payment
(
    payment_id         BIGSERIAL PRIMARY KEY,
    account_name_owner TEXT                                  NOT NULL,
    transaction_date   DATE                                  NOT NULL,
    amount             NUMERIC(8, 2) DEFAULT 0.00            NOT NULL,
    guid_source        TEXT                                  NOT NULL,
    guid_destination   TEXT                                  NOT NULL,
    owner              TEXT                                  NULL,
    active_status      BOOLEAN       DEFAULT TRUE            NOT NULL,
    date_updated       TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added         TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT payment_constraint UNIQUE (account_name_owner, transaction_date, amount),
    CONSTRAINT fk_payment_guid_source FOREIGN KEY (guid_source) REFERENCES public.t_transaction (guid) ON DELETE CASCADE,
    CONSTRAINT fk_payment_guid_destination FOREIGN KEY (guid_destination) REFERENCES public.t_transaction (guid) ON DELETE CASCADE
);

-- ALTER TABLE public.t_payment drop constraint fk_guid_source, add CONSTRAINT fk_guid_source FOREIGN KEY (guid_source) REFERENCES public.t_transaction (guid) ON DELETE CASCADE;
-- ALTER TABLE public.t_payment drop constraint fk_guid_destination, add CONSTRAINT fk_guid_destination FOREIGN KEY (guid_destination) REFERENCES public.t_transaction (guid) ON DELETE CASCADE;


--------------
-- Transfer --
--------------
CREATE TABLE IF NOT EXISTS public.t_transfer
(
    transfer_id         BIGSERIAL PRIMARY KEY,
    source_account      TEXT                                  NOT NULL,
    destination_account TEXT                                  NOT NULL,
    transaction_date    DATE                                  NOT NULL,
    amount              NUMERIC(8, 2) DEFAULT 0.00            NOT NULL,
    guid_source         TEXT                                  NOT NULL,
    guid_destination    TEXT                                  NOT NULL,
    owner               TEXT                                  NULL,
    active_status       BOOLEAN       DEFAULT TRUE            NOT NULL,
    date_updated        TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added          TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT transfer_constraint UNIQUE (source_account, destination_account, transaction_date, amount),
    CONSTRAINT fk_transfer_guid_source FOREIGN KEY (guid_source) REFERENCES public.t_transaction (guid) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_guid_destination FOREIGN KEY (guid_destination) REFERENCES public.t_transaction (guid) ON DELETE CASCADE
);

------------------
-- Parameter    --
------------------
CREATE TABLE IF NOT EXISTS public.t_parameter
(
    parameter_id       BIGSERIAL PRIMARY KEY,
    parameter_name     TEXT UNIQUE                       NOT NULL,
    parameter_value    TEXT                              NOT NULL,
    owner              TEXT                              NULL,
    active_status      BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated       TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added         TIMESTAMP                         NOT NULL DEFAULT TO_TIMESTAMP(0)
);

-- ALTER TABLE public.t_parameter ADD COLUMN active_status BOOLEAN NOT NULL DEFAULT TRUE;
-- INSERT into t_parameter(parameter_name, parameter_value) VALUES('payment_account', '');

-----------------
-- description --
-----------------
CREATE TABLE IF NOT EXISTS public.t_description
(
    description_id BIGSERIAL PRIMARY KEY,
    description_name    TEXT UNIQUE                       NOT NULL,
    owner               TEXT                              NULL,
    active_status       BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated        TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added          TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT t_description_description_lowercase_ck CHECK (description_name = lower(description_name))
);

-- ALTER TABLE public.t_description ADD COLUMN active_status      BOOLEAN        NOT NULL DEFAULT TRUE;
-- ALTER TABLE public.t_description RENAME COLUMN description TO description_name;

SELECT setval('public.t_receipt_image_receipt_image_id_seq',
              (SELECT MAX(receipt_image_id) FROM public.t_receipt_image) + 1);
SELECT setval('public.t_transaction_transaction_id_seq', (SELECT MAX(transaction_id) FROM public.t_transaction) + 1);
SELECT setval('public.t_payment_payment_id_seq', (SELECT MAX(payment_id) FROM public.t_payment) + 1);
SELECT setval('public.t_account_account_id_seq', (SELECT MAX(account_id) FROM public.t_account) + 1);
SELECT setval('public.t_category_category_id_seq', (SELECT MAX(category_id) FROM public.t_category) + 1);
SELECT setval('public.t_description_description_id_seq', (SELECT MAX(description_id) FROM public.t_description) + 1);
SELECT setval('public.t_parameter_parameter_id_seq', (SELECT MAX(parameter_id) FROM public.t_parameter) + 1);
SELECT setval('public.t_validation_amount_validation_id_seq',
              (SELECT MAX(validation_id) FROM public.t_validation_amount) + 1);

CREATE OR REPLACE FUNCTION fn_update_transaction_categories()
    RETURNS TRIGGER
    SET SCHEMA 'public'
    LANGUAGE PLPGSQL
AS
$$
    BEGIN
      NEW.date_updated := CURRENT_TIMESTAMP;
      RETURN NEW;
    END;
$$;

CREATE OR REPLACE FUNCTION fn_insert_transaction_categories()
    RETURNS TRIGGER
    SET SCHEMA 'public'
    LANGUAGE PLPGSQL
AS
$$
    BEGIN
      NEW.date_updated := CURRENT_TIMESTAMP;
      NEW.date_added := CURRENT_TIMESTAMP;
      RETURN NEW;
    END;
$$;


CREATE OR REPLACE FUNCTION rename_account_owner(
    p_old_name VARCHAR,
    p_new_name VARCHAR
)
RETURNS VOID
SET SCHEMA 'public'
LANGUAGE PLPGSQL
AS
$$
BEGIN
    EXECUTE 'ALTER TABLE t_transaction DISABLE TRIGGER ALL';

    EXECUTE 'UPDATE t_transaction SET account_name_owner = $1 WHERE account_name_owner = $2'
    USING p_new_name, p_old_name;

    EXECUTE 'UPDATE t_account SET account_name_owner = $1 WHERE account_name_owner = $2'
    USING p_new_name, p_old_name;

    EXECUTE 'ALTER TABLE t_transaction ENABLE TRIGGER ALL';
END;
$$;


CREATE OR REPLACE FUNCTION disable_account_owner(
    p_new_name VARCHAR
)
RETURNS VOID
SET SCHEMA 'public'
LANGUAGE PLPGSQL
AS
$$
BEGIN
    EXECUTE 'ALTER TABLE t_transaction DISABLE TRIGGER ALL';

    EXECUTE 'UPDATE t_transaction SET active_status = false WHERE account_name_owner = $1'
    USING p_new_name;

    EXECUTE 'UPDATE t_account SET active_status = false WHERE account_name_owner = $1'
    USING p_new_name;

    EXECUTE 'ALTER TABLE t_transaction ENABLE TRIGGER ALL';
END;
$$;


DROP TRIGGER IF EXISTS tr_insert_transaction_categories ON public.t_transaction_categories;
CREATE TRIGGER tr_insert_transaction_categories
    BEFORE INSERT
    ON public.t_transaction_categories
    FOR EACH ROW
EXECUTE PROCEDURE fn_insert_transaction_categories();

DROP TRIGGER IF EXISTS tr_update_transaction_categories ON public.t_transaction_categories;
CREATE TRIGGER tr_update_transaction_categories
    BEFORE UPDATE
    ON public.t_transaction_categories
    FOR EACH ROW
EXECUTE PROCEDURE fn_update_transaction_categories();

COMMIT;
-- check for locks
-- SELECT pid, usename, pg_blocking_pids(pid) as blocked_by, query as blocked_query from pg_stat_activity where cardinality(pg_blocking_pids(pid)) > 0;

--SELECT * from t_transaction where transaction_state = 'cleared' and transaction_date > now();
--SELECT * from t_transaction where transaction_state in ('future', 'outstanding') and transaction_date < now();
