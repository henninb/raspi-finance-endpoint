-- V19: Consolidated schema - final state of V01 through V18
-- For fresh databases: delete V01-V18, rename this to V01, and run.
-- For existing databases: run `flyway baseline -baselineVersion=19` to skip this migration.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SET client_min_messages TO WARNING;

CREATE SCHEMA IF NOT EXISTS public;

BEGIN;

-------------
-- Account --
-------------
CREATE TABLE IF NOT EXISTS public.t_account
(
    account_id         BIGSERIAL PRIMARY KEY,
    account_name_owner TEXT                                  NOT NULL,
    account_name       TEXT                                  NULL,
    account_owner      TEXT                                  NULL,
    account_type       TEXT          DEFAULT 'unknown'       NOT NULL,
    active_status      BOOLEAN       DEFAULT TRUE            NOT NULL,
    payment_required   BOOLEAN                               NULL DEFAULT TRUE,
    moniker            TEXT          DEFAULT '0000'          NOT NULL,
    future             NUMERIC(12, 2) DEFAULT 0.00           NULL,
    outstanding        NUMERIC(12, 2) DEFAULT 0.00           NULL,
    cleared            NUMERIC(12, 2) DEFAULT 0.00           NULL,
    date_closed        TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    validation_date    TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    owner              TEXT                                  NOT NULL,
    date_updated       TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added         TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT unique_owner_account_name_owner_account_type UNIQUE (owner, account_name_owner, account_type),
    CONSTRAINT unique_owner_account_name_owner UNIQUE (owner, account_name_owner),
    CONSTRAINT unique_owner_account_id_name_type UNIQUE (owner, account_id, account_name_owner, account_type),
    CONSTRAINT unique_owner_account_id UNIQUE (owner, account_id),
    CONSTRAINT ck_account_type CHECK (account_type IN (
        'credit', 'debit', 'undefined',
        'checking', 'savings', 'credit_card', 'certificate', 'money_market',
        'brokerage', 'retirement_401k', 'retirement_ira', 'retirement_roth', 'pension',
        'hsa', 'fsa', 'medical_savings',
        'mortgage', 'auto_loan', 'student_loan', 'personal_loan', 'line_of_credit',
        'utility', 'prepaid', 'gift_card',
        'business_checking', 'business_savings', 'business_credit',
        'cash', 'escrow', 'trust'
    )),
    CONSTRAINT ck_account_type_lowercase CHECK (account_type = lower(account_type))
);

CREATE INDEX IF NOT EXISTS idx_account_type ON public.t_account(account_type);
CREATE INDEX IF NOT EXISTS idx_account_active_type ON public.t_account(active_status, account_type) WHERE active_status = true;
CREATE INDEX IF NOT EXISTS idx_account_owner ON public.t_account(owner);

----------------------------
-- Validation Amount Date --
----------------------------
CREATE TABLE IF NOT EXISTS public.t_validation_amount
(
    validation_id     BIGSERIAL PRIMARY KEY,
    account_id        BIGINT                                NOT NULL,
    validation_date   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT '1970-01-01 00:00:00'::TIMESTAMP,
    transaction_state TEXT          DEFAULT 'undefined'     NOT NULL,
    amount            NUMERIC(12, 2) DEFAULT 0.00           NOT NULL,
    owner             TEXT                                  NOT NULL,
    active_status     BOOLEAN       DEFAULT TRUE            NOT NULL,
    date_updated      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    date_added        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT ck_transaction_state CHECK (transaction_state IN ('outstanding', 'future', 'cleared', 'undefined')),
    CONSTRAINT fk_account_id FOREIGN KEY (owner, account_id) REFERENCES public.t_account (owner, account_id) ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_validation_amount_owner ON public.t_validation_amount(owner);

----------
-- User --
----------
CREATE TABLE IF NOT EXISTS public.t_user
(
    user_id       BIGSERIAL PRIMARY KEY,
    username      TEXT UNIQUE                       NOT NULL,
    password      TEXT                              NOT NULL,
    first_name    TEXT                              NOT NULL,
    last_name     TEXT                              NOT NULL,
    active_status BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated  TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added    TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT ck_lowercase_username CHECK (username = lower(username))
);

----------
-- Role --
----------
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
    category_name TEXT                              NOT NULL,
    owner         TEXT                              NOT NULL,
    active_status BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated  TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added    TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT unique_owner_category_name UNIQUE (owner, category_name),
    CONSTRAINT ck_lowercase_category CHECK (category_name = lower(category_name))
);

CREATE INDEX IF NOT EXISTS idx_category_owner ON public.t_category(owner);

-----------------
-- Description --
-----------------
CREATE TABLE IF NOT EXISTS public.t_description
(
    description_id BIGSERIAL PRIMARY KEY,
    description_name    TEXT                              NOT NULL,
    owner               TEXT                              NOT NULL,
    active_status       BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated        TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added          TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT unique_owner_description_name UNIQUE (owner, description_name),
    CONSTRAINT t_description_description_lowercase_ck CHECK (description_name = lower(description_name))
);

CREATE INDEX IF NOT EXISTS idx_description_owner ON public.t_description(owner);

---------------------------
-- TransactionCategories --
---------------------------
CREATE TABLE IF NOT EXISTS public.t_transaction_categories
(
    category_id    BIGINT                            NOT NULL,
    transaction_id BIGINT                            NOT NULL,
    owner          TEXT                              NOT NULL,
    date_updated   TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added     TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    PRIMARY KEY (category_id, transaction_id)
);

CREATE INDEX IF NOT EXISTS idx_transaction_categories_owner ON public.t_transaction_categories(owner);

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
    owner             TEXT                              NOT NULL,
    active_status     BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated      TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added        TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT ck_image_size CHECK (length(image) <= 1048576),
    CONSTRAINT ck_image_type CHECK (image_format_type IN ('jpeg', 'png', 'undefined'))
);

CREATE INDEX IF NOT EXISTS idx_receipt_image_owner ON public.t_receipt_image(owner);

-----------------
-- Transaction --
-----------------
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
    amount             NUMERIC(12, 2) DEFAULT 0.00           NOT NULL,
    transaction_state  TEXT          DEFAULT 'undefined'     NOT NULL,
    reoccurring_type   TEXT          DEFAULT 'undefined'     NULL,
    active_status      BOOLEAN       DEFAULT TRUE            NOT NULL,
    notes              TEXT          DEFAULT ''              NOT NULL,
    receipt_image_id   BIGINT                                NULL,
    owner              TEXT                                  NOT NULL,
    date_updated       TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added         TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT unique_owner_transaction UNIQUE (owner, account_name_owner, transaction_date, description, category, amount, notes),
    CONSTRAINT unique_owner_transaction_id UNIQUE (owner, transaction_id),
    CONSTRAINT t_transaction_description_lowercase_ck CHECK (description = lower(description)),
    CONSTRAINT t_transaction_category_lowercase_ck CHECK (category = lower(category)),
    CONSTRAINT t_transaction_notes_lowercase_ck CHECK (notes = lower(notes)),
    CONSTRAINT ck_transaction_state CHECK (transaction_state IN ('outstanding', 'future', 'cleared', 'undefined')),
    CONSTRAINT ck_account_type CHECK (account_type IN ('debit', 'credit', 'undefined')),
    CONSTRAINT ck_transaction_type CHECK (transaction_type IN ('expense', 'income', 'transfer', 'undefined')),
    CONSTRAINT ck_reoccurring_type CHECK (reoccurring_type IN
                                          ('annually', 'biannually', 'fortnightly', 'monthly', 'quarterly', 'onetime',
                                           'undefined')),
    CONSTRAINT fk_account_id_account_name_owner FOREIGN KEY (owner, account_id, account_name_owner, account_type) REFERENCES public.t_account (owner, account_id, account_name_owner, account_type) ON UPDATE CASCADE,
    CONSTRAINT fk_receipt_image FOREIGN KEY (receipt_image_id) REFERENCES public.t_receipt_image (receipt_image_id) ON UPDATE CASCADE,
    CONSTRAINT fk_category_name FOREIGN KEY (owner, category) REFERENCES public.t_category (owner, category_name) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_description_name FOREIGN KEY (owner, description) REFERENCES public.t_description (owner, description_name) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- Add compound FK from t_receipt_image to t_transaction (circular reference, must be added after t_transaction exists)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_transaction' AND conrelid = 'public.t_receipt_image'::regclass
    ) THEN
        ALTER TABLE public.t_receipt_image
            ADD CONSTRAINT fk_transaction FOREIGN KEY (owner, transaction_id) REFERENCES public.t_transaction (owner, transaction_id) ON UPDATE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_transaction_owner ON public.t_transaction(owner);
CREATE INDEX IF NOT EXISTS idx_transaction_account_lookup ON public.t_transaction(account_name_owner, active_status, transaction_date DESC);

--------------------------
-- Pending Transaction  --
--------------------------
CREATE TABLE IF NOT EXISTS public.t_pending_transaction
(
    pending_transaction_id BIGSERIAL PRIMARY KEY,
    account_name_owner     TEXT                              NOT NULL,
    transaction_date       DATE                              NOT NULL,
    description            TEXT                              NOT NULL,
    amount                 NUMERIC(12, 2) DEFAULT 0.00       NOT NULL,
    review_status          TEXT          DEFAULT 'pending'   NOT NULL,
    owner                  TEXT                              NOT NULL,
    date_added             TIMESTAMP     DEFAULT now()       NOT NULL,
    CONSTRAINT unique_owner_pending_transaction UNIQUE (owner, account_name_owner, transaction_date, description, amount),
    CONSTRAINT fk_pending_account FOREIGN KEY (owner, account_name_owner) REFERENCES public.t_account (owner, account_name_owner) ON UPDATE CASCADE,
    CONSTRAINT ck_review_status CHECK (review_status IN ('pending', 'approved', 'rejected'))
);

CREATE INDEX IF NOT EXISTS idx_pending_transaction_owner ON public.t_pending_transaction(owner);

-------------
-- Payment --
-------------
CREATE TABLE IF NOT EXISTS public.t_payment
(
    payment_id           BIGSERIAL PRIMARY KEY,
    source_account       TEXT                                  NOT NULL,
    destination_account  TEXT                                  NOT NULL,
    transaction_date     DATE                                  NOT NULL,
    amount               NUMERIC(12, 2) DEFAULT 0.00           NOT NULL,
    guid_source          TEXT                                  NOT NULL,
    guid_destination     TEXT                                  NOT NULL,
    owner                TEXT                                  NOT NULL,
    active_status        BOOLEAN       DEFAULT TRUE            NOT NULL,
    date_updated         TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added           TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT unique_owner_payment UNIQUE (owner, destination_account, transaction_date, amount),
    CONSTRAINT fk_payment_guid_source FOREIGN KEY (guid_source) REFERENCES public.t_transaction (guid) ON UPDATE CASCADE,
    CONSTRAINT fk_payment_guid_destination FOREIGN KEY (guid_destination) REFERENCES public.t_transaction (guid) ON UPDATE CASCADE,
    CONSTRAINT fk_payment_source_account FOREIGN KEY (owner, source_account) REFERENCES public.t_account (owner, account_name_owner) ON UPDATE CASCADE,
    CONSTRAINT fk_payment_destination_account FOREIGN KEY (owner, destination_account) REFERENCES public.t_account (owner, account_name_owner) ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_payment_owner ON public.t_payment(owner);

--------------
-- Transfer --
--------------
CREATE TABLE IF NOT EXISTS public.t_transfer
(
    transfer_id         BIGSERIAL PRIMARY KEY,
    source_account      TEXT                                  NOT NULL,
    destination_account TEXT                                  NOT NULL,
    transaction_date    DATE                                  NOT NULL,
    amount              NUMERIC(12, 2) DEFAULT 0.00           NOT NULL,
    guid_source         TEXT                                  NOT NULL,
    guid_destination    TEXT                                  NOT NULL,
    owner               TEXT                                  NOT NULL,
    active_status       BOOLEAN       DEFAULT TRUE            NOT NULL,
    date_updated        TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added          TIMESTAMP     DEFAULT TO_TIMESTAMP(0) NOT NULL,
    CONSTRAINT unique_owner_transfer UNIQUE (owner, source_account, destination_account, transaction_date, amount),
    CONSTRAINT fk_transfer_guid_source FOREIGN KEY (guid_source) REFERENCES public.t_transaction (guid) ON UPDATE CASCADE,
    CONSTRAINT fk_transfer_guid_destination FOREIGN KEY (guid_destination) REFERENCES public.t_transaction (guid) ON UPDATE CASCADE,
    CONSTRAINT fk_source_account FOREIGN KEY (owner, source_account) REFERENCES public.t_account (owner, account_name_owner) ON UPDATE CASCADE,
    CONSTRAINT fk_destination_account FOREIGN KEY (owner, destination_account) REFERENCES public.t_account (owner, account_name_owner) ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_transfer_owner ON public.t_transfer(owner);

---------------
-- Parameter --
---------------
CREATE TABLE IF NOT EXISTS public.t_parameter
(
    parameter_id       BIGSERIAL PRIMARY KEY,
    parameter_name     TEXT                              NOT NULL,
    parameter_value    TEXT                              NOT NULL,
    owner              TEXT                              NOT NULL,
    active_status      BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated       TIMESTAMP DEFAULT TO_TIMESTAMP(0) NOT NULL,
    date_added         TIMESTAMP                         NOT NULL DEFAULT TO_TIMESTAMP(0),
    CONSTRAINT unique_owner_parameter_name UNIQUE (owner, parameter_name)
);

CREATE INDEX IF NOT EXISTS idx_parameter_owner ON public.t_parameter(owner);

----------------------
-- Medical Provider --
----------------------
CREATE TABLE IF NOT EXISTS public.t_medical_provider (
    provider_id         BIGSERIAL PRIMARY KEY,
    provider_name       TEXT NOT NULL,
    provider_type       TEXT NOT NULL DEFAULT 'general',
    specialty           TEXT,
    npi                 TEXT UNIQUE,
    tax_id              TEXT,

    address_line1       TEXT,
    address_line2       TEXT,
    city               TEXT,
    state              TEXT,
    zip_code           TEXT,
    country            TEXT DEFAULT 'US',

    phone              TEXT,
    fax                TEXT,
    email              TEXT,
    website            TEXT,

    network_status     TEXT DEFAULT 'unknown',
    billing_name       TEXT,
    notes              TEXT,

    active_status      BOOLEAN DEFAULT TRUE NOT NULL,
    date_added         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_updated       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT ck_provider_type CHECK (provider_type IN (
        'general', 'specialist', 'hospital', 'pharmacy', 'laboratory',
        'imaging', 'urgent_care', 'emergency', 'mental_health', 'dental',
        'vision', 'physical_therapy', 'other'
    )),
    CONSTRAINT ck_network_status CHECK (network_status IN (
        'in_network', 'out_of_network', 'unknown'
    )),
    CONSTRAINT ck_provider_name_lowercase CHECK (provider_name = lower(provider_name)),
    CONSTRAINT ck_provider_name_not_empty CHECK (length(trim(provider_name)) > 0),
    CONSTRAINT ck_npi_format CHECK (npi IS NULL OR (npi ~ '^[0-9]{10}$')),
    CONSTRAINT ck_zip_code_format CHECK (zip_code IS NULL OR (zip_code ~ '^[0-9]{5}(-[0-9]{4})?$')),
    CONSTRAINT ck_phone_format CHECK (phone IS NULL OR (length(phone) >= 10))
);

CREATE INDEX IF NOT EXISTS idx_medical_provider_name ON public.t_medical_provider(provider_name);
CREATE INDEX IF NOT EXISTS idx_medical_provider_type ON public.t_medical_provider(provider_type);
CREATE INDEX IF NOT EXISTS idx_medical_provider_specialty ON public.t_medical_provider(specialty) WHERE specialty IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_medical_provider_npi ON public.t_medical_provider(npi) WHERE npi IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_medical_provider_active ON public.t_medical_provider(active_status, provider_name) WHERE active_status = true;
CREATE INDEX IF NOT EXISTS idx_medical_provider_network ON public.t_medical_provider(network_status, provider_type);
CREATE INDEX IF NOT EXISTS idx_medical_provider_location ON public.t_medical_provider(state, city) WHERE state IS NOT NULL AND city IS NOT NULL;

INSERT INTO public.t_medical_provider (provider_name, provider_type, specialty, network_status)
SELECT v.provider_name, v.provider_type, v.specialty, v.network_status
FROM (VALUES
    ('unknown_provider', 'general', NULL::TEXT, 'unknown'),
    ('pharmacy_generic', 'pharmacy', 'retail_pharmacy', 'unknown'),
    ('urgent_care_generic', 'urgent_care', NULL::TEXT, 'unknown'),
    ('hospital_generic', 'hospital', NULL::TEXT, 'unknown'),
    ('laboratory_generic', 'laboratory', 'general_lab', 'unknown')
) AS v(provider_name, provider_type, specialty, network_status)
WHERE NOT EXISTS (
    SELECT 1 FROM public.t_medical_provider p WHERE p.provider_name = v.provider_name
);

-------------------
-- Family Member --
-------------------
CREATE TABLE IF NOT EXISTS public.t_family_member (
    family_member_id    BIGSERIAL PRIMARY KEY,
    owner               TEXT NOT NULL,
    member_name         TEXT NOT NULL,
    relationship        TEXT NOT NULL DEFAULT 'self',
    date_of_birth       DATE,
    insurance_member_id TEXT,

    ssn_last_four      TEXT,
    medical_record_number TEXT,

    active_status      BOOLEAN DEFAULT TRUE NOT NULL,
    date_added         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_updated       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_family_member_owner_name UNIQUE (owner, member_name),
    CONSTRAINT unique_owner_family_member_id UNIQUE (owner, family_member_id),
    CONSTRAINT ck_family_relationship CHECK (relationship IN (
        'self', 'spouse', 'child', 'dependent', 'other'
    )),
    CONSTRAINT ck_family_member_name_lowercase CHECK (member_name = lower(member_name)),
    CONSTRAINT ck_family_owner_lowercase CHECK (owner = lower(owner)),
    CONSTRAINT ck_family_member_name_not_empty CHECK (length(trim(member_name)) > 0),
    CONSTRAINT ck_family_owner_not_empty CHECK (length(trim(owner)) > 0),
    CONSTRAINT ck_ssn_last_four_format CHECK (ssn_last_four IS NULL OR (ssn_last_four ~ '^[0-9]{4}$')),
    CONSTRAINT ck_insurance_member_id_length CHECK (insurance_member_id IS NULL OR length(insurance_member_id) <= 50),
    CONSTRAINT ck_medical_record_number_length CHECK (medical_record_number IS NULL OR length(medical_record_number) <= 50)
);

CREATE INDEX IF NOT EXISTS idx_family_member_owner ON public.t_family_member(owner);
CREATE INDEX IF NOT EXISTS idx_family_member_relationship ON public.t_family_member(owner, relationship);
CREATE INDEX IF NOT EXISTS idx_family_member_active ON public.t_family_member(active_status, owner) WHERE active_status = true;
CREATE INDEX IF NOT EXISTS idx_family_member_insurance ON public.t_family_member(insurance_member_id) WHERE insurance_member_id IS NOT NULL;

---------------------
-- Medical Expense --
---------------------
CREATE TABLE IF NOT EXISTS public.t_medical_expense (
    medical_expense_id          BIGSERIAL PRIMARY KEY,
    transaction_id              BIGINT,
    provider_id                 BIGINT,
    family_member_id            BIGINT,

    service_date                DATE NOT NULL,
    service_description         TEXT,
    procedure_code              TEXT,
    diagnosis_code              TEXT,

    billed_amount              NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    insurance_discount         NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    insurance_paid             NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    patient_responsibility     NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    paid_amount                NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    paid_date                  DATE,

    is_out_of_network          BOOLEAN DEFAULT FALSE NOT NULL,
    claim_number               TEXT,
    claim_status               TEXT DEFAULT 'submitted' NOT NULL,

    owner                      TEXT NOT NULL,
    active_status              BOOLEAN DEFAULT TRUE NOT NULL,
    date_added                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_updated               TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_medical_expense_transaction FOREIGN KEY (owner, transaction_id)
        REFERENCES public.t_transaction(owner, transaction_id) ON DELETE CASCADE,
    CONSTRAINT fk_medical_expense_provider FOREIGN KEY (provider_id)
        REFERENCES public.t_medical_provider(provider_id),
    CONSTRAINT fk_medical_expense_family_member FOREIGN KEY (owner, family_member_id)
        REFERENCES public.t_family_member (owner, family_member_id) ON UPDATE CASCADE,
    CONSTRAINT uk_medical_expense_transaction UNIQUE (transaction_id),
    CONSTRAINT ck_medical_expense_claim_status CHECK (claim_status IN (
        'submitted', 'processing', 'approved', 'denied', 'paid', 'closed'
    )),
    CONSTRAINT ck_medical_expense_financial_amounts CHECK (
        billed_amount >= 0 AND
        insurance_discount >= 0 AND
        insurance_paid >= 0 AND
        patient_responsibility >= 0
    ),
    CONSTRAINT ck_paid_amount_non_negative CHECK (paid_amount >= 0),
    CONSTRAINT ck_medical_expense_service_date_valid CHECK (service_date <= CURRENT_DATE),
    CONSTRAINT ck_medical_expense_financial_consistency CHECK (
        billed_amount >= (insurance_discount + insurance_paid + patient_responsibility)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_medical_expense_transaction ON public.t_medical_expense(transaction_id);
CREATE INDEX IF NOT EXISTS idx_medical_expense_provider ON public.t_medical_expense(provider_id);
CREATE INDEX IF NOT EXISTS idx_medical_expense_family_member ON public.t_medical_expense(family_member_id);
CREATE INDEX IF NOT EXISTS idx_medical_expense_service_date ON public.t_medical_expense(service_date);
CREATE INDEX IF NOT EXISTS idx_medical_expense_claim_number ON public.t_medical_expense(claim_number) WHERE claim_number IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_medical_expense_claim_status ON public.t_medical_expense(claim_status);
CREATE INDEX IF NOT EXISTS idx_medical_expense_active ON public.t_medical_expense(active_status, service_date);
CREATE INDEX IF NOT EXISTS idx_medical_expense_owner ON public.t_medical_expense(owner);

COMMENT ON TABLE public.t_medical_expense IS 'Medical expenses linked to transactions with comprehensive tracking';
COMMENT ON COLUMN public.t_medical_expense.transaction_id IS 'Optional reference to payment transaction, can be null for unpaid expenses';
COMMENT ON COLUMN public.t_medical_expense.provider_id IS 'Foreign key to t_medical_provider';
COMMENT ON COLUMN public.t_medical_expense.family_member_id IS 'Foreign key to t_family_member for tracking which family member';
COMMENT ON COLUMN public.t_medical_expense.service_date IS 'Date medical service was provided (different from payment date)';
COMMENT ON COLUMN public.t_medical_expense.billed_amount IS 'Original amount billed by provider';
COMMENT ON COLUMN public.t_medical_expense.insurance_discount IS 'Insurance negotiated discount amount';
COMMENT ON COLUMN public.t_medical_expense.insurance_paid IS 'Amount paid by insurance';
COMMENT ON COLUMN public.t_medical_expense.patient_responsibility IS 'Amount patient is responsible to pay';
COMMENT ON COLUMN public.t_medical_expense.paid_amount IS 'Actual amount paid by patient, synced with linked transaction amount';
COMMENT ON COLUMN public.t_medical_expense.is_out_of_network IS 'Whether provider is out of insurance network';
COMMENT ON COLUMN public.t_medical_expense.claim_status IS 'Status of insurance claim processing';

-----------------------------------------------
-- Functions
-----------------------------------------------

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
      NEW.owner := (SELECT owner FROM t_transaction WHERE transaction_id = NEW.transaction_id);
      NEW.date_updated := CURRENT_TIMESTAMP;
      NEW.date_added := CURRENT_TIMESTAMP;
      RETURN NEW;
    END;
$$;

CREATE OR REPLACE FUNCTION rename_account_owner(
    p_old_name VARCHAR,
    p_new_name VARCHAR,
    p_owner VARCHAR
)
RETURNS VOID
SET SCHEMA 'public'
LANGUAGE PLPGSQL
AS
$$
BEGIN
    EXECUTE 'ALTER TABLE t_transaction DISABLE TRIGGER ALL';

    EXECUTE 'UPDATE t_transaction SET account_name_owner = $1 WHERE account_name_owner = $2 AND owner = $3'
    USING p_new_name, p_old_name, p_owner;

    EXECUTE 'UPDATE t_account SET account_name_owner = $1 WHERE account_name_owner = $2 AND owner = $3'
    USING p_new_name, p_old_name, p_owner;

    EXECUTE 'ALTER TABLE t_transaction ENABLE TRIGGER ALL';
END;
$$;

CREATE OR REPLACE FUNCTION disable_account_owner(
    p_new_name VARCHAR,
    p_owner VARCHAR
)
RETURNS VOID
SET SCHEMA 'public'
LANGUAGE PLPGSQL
AS
$$
BEGIN
    EXECUTE 'ALTER TABLE t_transaction DISABLE TRIGGER ALL';

    EXECUTE 'UPDATE t_transaction SET active_status = false WHERE account_name_owner = $1 AND owner = $2'
    USING p_new_name, p_owner;

    EXECUTE 'UPDATE t_account SET active_status = false WHERE account_name_owner = $1 AND owner = $2'
    USING p_new_name, p_owner;

    EXECUTE 'ALTER TABLE t_transaction ENABLE TRIGGER ALL';
END;
$$;

-----------------------------------------------
-- Triggers
-----------------------------------------------

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
