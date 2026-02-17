-- V01: Consolidated schema for functional tests (H2)
-- Based on prod V19 consolidated schema, adapted for H2 syntax.

CREATE SCHEMA IF NOT EXISTS func;

-------------
-- Account --
-------------
CREATE TABLE IF NOT EXISTS func.t_account
(
    account_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    date_closed        TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    validation_date    TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    owner              TEXT                                  NOT NULL,
    date_updated       TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added         TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
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

CREATE INDEX IF NOT EXISTS idx_account_type ON func.t_account(account_type);
CREATE INDEX IF NOT EXISTS idx_account_active_type ON func.t_account(active_status, account_type);
CREATE INDEX IF NOT EXISTS idx_account_owner ON func.t_account(owner);

----------------------------
-- Validation Amount Date --
----------------------------
CREATE TABLE IF NOT EXISTS func.t_validation_amount
(
    validation_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id        BIGINT                                NOT NULL,
    validation_date   TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    transaction_state TEXT          DEFAULT 'undefined'     NOT NULL,
    amount            NUMERIC(12, 2) DEFAULT 0.00           NOT NULL,
    owner             TEXT                                  NOT NULL,
    active_status     BOOLEAN       DEFAULT TRUE            NOT NULL,
    date_updated      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_added        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_validation_transaction_state CHECK (transaction_state IN ('outstanding', 'future', 'cleared', 'undefined')),
    CONSTRAINT fk_validation_account_id FOREIGN KEY (owner, account_id) REFERENCES func.t_account (owner, account_id) ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_validation_amount_owner ON func.t_validation_amount(owner);

----------
-- User --
----------
CREATE TABLE IF NOT EXISTS func.t_user
(
    user_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      TEXT UNIQUE                       NOT NULL,
    password      TEXT                              NOT NULL,
    first_name    TEXT                              NOT NULL,
    last_name     TEXT                              NOT NULL,
    active_status BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated  TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added    TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT ck_lowercase_username CHECK (username = lower(username))
);

----------
-- Role --
----------
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
    category_name TEXT                              NOT NULL,
    owner         TEXT                              NOT NULL,
    active_status BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated  TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added    TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT unique_owner_category_name UNIQUE (owner, category_name),
    CONSTRAINT ck_lowercase_category CHECK (category_name = lower(category_name))
);

CREATE INDEX IF NOT EXISTS idx_category_owner ON func.t_category(owner);

-----------------
-- Description --
-----------------
CREATE TABLE IF NOT EXISTS func.t_description
(
    description_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    description_name TEXT                              NOT NULL,
    owner            TEXT                              NOT NULL,
    active_status    BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated     TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added       TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT unique_owner_description_name UNIQUE (owner, description_name),
    CONSTRAINT t_description_description_lowercase_ck CHECK (description_name = lower(description_name))
);

CREATE INDEX IF NOT EXISTS idx_description_owner ON func.t_description(owner);

---------------------------
-- TransactionCategories --
---------------------------
CREATE TABLE IF NOT EXISTS func.t_transaction_categories
(
    category_id    BIGINT                            NOT NULL,
    transaction_id BIGINT                            NOT NULL,
    owner          TEXT                              NOT NULL,
    date_updated   TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added     TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    PRIMARY KEY (category_id, transaction_id)
);

CREATE INDEX IF NOT EXISTS idx_transaction_categories_owner ON func.t_transaction_categories(owner);

-------------------
-- ReceiptImage  --
-------------------
CREATE TABLE IF NOT EXISTS func.t_receipt_image
(
    receipt_image_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id    BIGINT                            NOT NULL,
    image             BLOB                              NOT NULL,
    thumbnail         BLOB                              NOT NULL,
    image_format_type TEXT      DEFAULT 'undefined'     NOT NULL,
    owner             TEXT                              NOT NULL,
    active_status     BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated      TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added        TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT ck_image_size CHECK (length(image) <= 1048576),
    CONSTRAINT ck_image_type CHECK (image_format_type IN ('jpeg', 'png', 'undefined'))
);

CREATE INDEX IF NOT EXISTS idx_receipt_image_owner ON func.t_receipt_image(owner);

-----------------
-- Transaction --
-----------------
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
    amount             NUMERIC(12, 2) DEFAULT 0.00           NOT NULL,
    transaction_state  TEXT          DEFAULT 'undefined'     NOT NULL,
    reoccurring_type   TEXT          DEFAULT 'undefined'     NULL,
    active_status      BOOLEAN       DEFAULT TRUE            NOT NULL,
    notes              TEXT          DEFAULT ''              NOT NULL,
    receipt_image_id   BIGINT                                NULL,
    owner              TEXT                                  NOT NULL,
    date_updated       TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added         TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT unique_owner_transaction UNIQUE (owner, account_name_owner, transaction_date, description, category, amount, notes),
    CONSTRAINT unique_owner_transaction_id UNIQUE (owner, transaction_id),
    CONSTRAINT t_transaction_description_lowercase_ck CHECK (description = lower(description)),
    CONSTRAINT t_transaction_category_lowercase_ck CHECK (category = lower(category)),
    CONSTRAINT t_transaction_notes_lowercase_ck CHECK (notes = lower(notes)),
    CONSTRAINT ck_transaction_transaction_state CHECK (transaction_state IN ('outstanding', 'future', 'cleared', 'undefined')),
    CONSTRAINT ck_transaction_account_type CHECK (account_type IN ('debit', 'credit', 'undefined')),
    CONSTRAINT ck_transaction_type CHECK (transaction_type IN ('expense', 'income', 'transfer', 'undefined')),
    CONSTRAINT ck_reoccurring_type CHECK (reoccurring_type IN
                                          ('annually', 'biannually', 'fortnightly', 'monthly', 'quarterly', 'onetime',
                                           'undefined')),
    CONSTRAINT fk_account_id_account_name_owner FOREIGN KEY (owner, account_id, account_name_owner, account_type) REFERENCES func.t_account (owner, account_id, account_name_owner, account_type) ON UPDATE CASCADE,
    CONSTRAINT fk_receipt_image FOREIGN KEY (receipt_image_id) REFERENCES func.t_receipt_image (receipt_image_id) ON UPDATE CASCADE,
    CONSTRAINT fk_category_name FOREIGN KEY (owner, category) REFERENCES func.t_category (owner, category_name) ON UPDATE CASCADE,
    CONSTRAINT fk_description_name FOREIGN KEY (owner, description) REFERENCES func.t_description (owner, description_name) ON UPDATE CASCADE
);

-- Circular FK: t_receipt_image -> t_transaction (must be added after t_transaction exists)
ALTER TABLE func.t_receipt_image
    DROP CONSTRAINT IF EXISTS fk_transaction;
ALTER TABLE func.t_receipt_image
    ADD CONSTRAINT fk_transaction FOREIGN KEY (owner, transaction_id) REFERENCES func.t_transaction (owner, transaction_id) ON UPDATE CASCADE;

CREATE INDEX IF NOT EXISTS idx_transaction_owner ON func.t_transaction(owner);
CREATE INDEX IF NOT EXISTS idx_transaction_account_lookup ON func.t_transaction(account_name_owner, active_status, transaction_date DESC);

--------------------------
-- Pending Transaction  --
--------------------------
CREATE TABLE IF NOT EXISTS func.t_pending_transaction
(
    pending_transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_name_owner     TEXT                              NOT NULL,
    transaction_date       DATE                              NOT NULL,
    description            TEXT                              NOT NULL,
    amount                 NUMERIC(12, 2) DEFAULT 0.00       NOT NULL,
    review_status          TEXT          DEFAULT 'pending'   NOT NULL,
    owner                  TEXT                              NOT NULL,
    date_added             TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_owner_pending_transaction UNIQUE (owner, account_name_owner, transaction_date, description, amount),
    CONSTRAINT fk_pending_account FOREIGN KEY (owner, account_name_owner) REFERENCES func.t_account (owner, account_name_owner) ON UPDATE CASCADE,
    CONSTRAINT ck_review_status CHECK (review_status IN ('pending', 'approved', 'rejected'))
);

CREATE INDEX IF NOT EXISTS idx_pending_transaction_owner ON func.t_pending_transaction(owner);

-------------
-- Payment --
-------------
CREATE TABLE IF NOT EXISTS func.t_payment
(
    payment_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_account       TEXT                                  NOT NULL,
    destination_account  TEXT                                  NOT NULL,
    transaction_date     DATE                                  NOT NULL,
    amount               NUMERIC(12, 2) DEFAULT 0.00           NOT NULL,
    guid_source          TEXT                                  NOT NULL,
    guid_destination     TEXT                                  NOT NULL,
    owner                TEXT                                  NOT NULL,
    active_status        BOOLEAN       DEFAULT TRUE            NOT NULL,
    date_updated         TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added           TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT unique_owner_payment UNIQUE (owner, destination_account, transaction_date, amount),
    CONSTRAINT fk_payment_guid_source FOREIGN KEY (guid_source) REFERENCES func.t_transaction (guid) ON UPDATE CASCADE,
    CONSTRAINT fk_payment_guid_destination FOREIGN KEY (guid_destination) REFERENCES func.t_transaction (guid) ON UPDATE CASCADE,
    CONSTRAINT fk_payment_source_account FOREIGN KEY (owner, source_account) REFERENCES func.t_account (owner, account_name_owner) ON UPDATE CASCADE,
    CONSTRAINT fk_payment_destination_account FOREIGN KEY (owner, destination_account) REFERENCES func.t_account (owner, account_name_owner) ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_payment_owner ON func.t_payment(owner);

--------------
-- Transfer --
--------------
CREATE TABLE IF NOT EXISTS func.t_transfer
(
    transfer_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_account      TEXT                                  NOT NULL,
    destination_account TEXT                                  NOT NULL,
    transaction_date    DATE                                  NOT NULL,
    amount              NUMERIC(12, 2) DEFAULT 0.00           NOT NULL,
    guid_source         TEXT                                  NOT NULL,
    guid_destination    TEXT                                  NOT NULL,
    owner               TEXT                                  NOT NULL,
    active_status       BOOLEAN       DEFAULT TRUE            NOT NULL,
    date_updated        TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added          TIMESTAMP     DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT unique_owner_transfer UNIQUE (owner, source_account, destination_account, transaction_date, amount),
    CONSTRAINT fk_transfer_guid_source FOREIGN KEY (guid_source) REFERENCES func.t_transaction (guid) ON UPDATE CASCADE,
    CONSTRAINT fk_transfer_guid_destination FOREIGN KEY (guid_destination) REFERENCES func.t_transaction (guid) ON UPDATE CASCADE,
    CONSTRAINT fk_source_account FOREIGN KEY (owner, source_account) REFERENCES func.t_account (owner, account_name_owner) ON UPDATE CASCADE,
    CONSTRAINT fk_destination_account FOREIGN KEY (owner, destination_account) REFERENCES func.t_account (owner, account_name_owner) ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_transfer_owner ON func.t_transfer(owner);

---------------
-- Parameter --
---------------
CREATE TABLE IF NOT EXISTS func.t_parameter
(
    parameter_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    parameter_name     TEXT                              NOT NULL,
    parameter_value    TEXT                              NOT NULL,
    owner              TEXT                              NOT NULL,
    active_status      BOOLEAN   DEFAULT TRUE            NOT NULL,
    date_updated       TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_added         TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    CONSTRAINT unique_owner_parameter_name UNIQUE (owner, parameter_name)
);

CREATE INDEX IF NOT EXISTS idx_parameter_owner ON func.t_parameter(owner);

----------------------
-- Medical Provider --
----------------------
CREATE TABLE IF NOT EXISTS func.t_medical_provider
(
    provider_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_name       TEXT NOT NULL,
    provider_type       TEXT NOT NULL DEFAULT 'general',
    specialty           TEXT,
    npi                 TEXT UNIQUE,
    tax_id              TEXT,
    address_line1       TEXT,
    address_line2       TEXT,
    city                TEXT,
    state               TEXT,
    zip_code            TEXT,
    country             TEXT DEFAULT 'US',
    phone               TEXT,
    fax                 TEXT,
    email               TEXT,
    website             TEXT,
    network_status      TEXT DEFAULT 'unknown',
    billing_name        TEXT,
    notes               TEXT,
    active_status       BOOLEAN DEFAULT TRUE NOT NULL,
    date_added          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_updated        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_provider_type CHECK (provider_type IN (
        'general', 'specialist', 'hospital', 'pharmacy', 'laboratory',
        'imaging', 'urgent_care', 'emergency', 'mental_health', 'dental',
        'vision', 'physical_therapy', 'other'
    )),
    CONSTRAINT ck_network_status CHECK (network_status IN (
        'in_network', 'out_of_network', 'unknown'
    )),
    CONSTRAINT ck_provider_name_lowercase CHECK (provider_name = lower(provider_name)),
    CONSTRAINT ck_provider_name_not_empty CHECK (length(trim(provider_name)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_medical_provider_name ON func.t_medical_provider(provider_name);
CREATE INDEX IF NOT EXISTS idx_medical_provider_type ON func.t_medical_provider(provider_type);
CREATE INDEX IF NOT EXISTS idx_medical_provider_network ON func.t_medical_provider(network_status, provider_type);

INSERT INTO func.t_medical_provider (provider_name, provider_type, specialty, network_status)
SELECT v.provider_name, v.provider_type, v.specialty, v.network_status
FROM (VALUES
    ('unknown_provider', 'general', CAST(NULL AS TEXT), 'unknown'),
    ('pharmacy_generic', 'pharmacy', 'retail_pharmacy', 'unknown'),
    ('urgent_care_generic', 'urgent_care', CAST(NULL AS TEXT), 'unknown'),
    ('hospital_generic', 'hospital', CAST(NULL AS TEXT), 'unknown'),
    ('laboratory_generic', 'laboratory', 'general_lab', 'unknown')
) AS v(provider_name, provider_type, specialty, network_status)
WHERE NOT EXISTS (
    SELECT 1 FROM func.t_medical_provider p WHERE p.provider_name = v.provider_name
);

-------------------
-- Family Member --
-------------------
CREATE TABLE IF NOT EXISTS func.t_family_member
(
    family_member_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner                 TEXT NOT NULL,
    member_name           TEXT NOT NULL,
    relationship          TEXT NOT NULL DEFAULT 'self',
    date_of_birth         DATE,
    insurance_member_id   TEXT,
    ssn_last_four         TEXT,
    medical_record_number TEXT,
    active_status         BOOLEAN DEFAULT TRUE NOT NULL,
    date_added            TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_updated          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_family_member_owner_name UNIQUE (owner, member_name),
    CONSTRAINT unique_owner_family_member_id UNIQUE (owner, family_member_id),
    CONSTRAINT ck_family_relationship CHECK (relationship IN (
        'self', 'spouse', 'child', 'dependent', 'other'
    )),
    CONSTRAINT ck_family_member_name_lowercase CHECK (member_name = lower(member_name)),
    CONSTRAINT ck_family_owner_lowercase CHECK (owner = lower(owner)),
    CONSTRAINT ck_family_member_name_not_empty CHECK (length(trim(member_name)) > 0),
    CONSTRAINT ck_family_owner_not_empty CHECK (length(trim(owner)) > 0),
    CONSTRAINT ck_insurance_member_id_length CHECK (insurance_member_id IS NULL OR length(insurance_member_id) <= 50),
    CONSTRAINT ck_medical_record_number_length CHECK (medical_record_number IS NULL OR length(medical_record_number) <= 50)
);

CREATE INDEX IF NOT EXISTS idx_family_member_owner ON func.t_family_member(owner);
CREATE INDEX IF NOT EXISTS idx_family_member_relationship ON func.t_family_member(owner, relationship);

---------------------
-- Medical Expense --
---------------------
CREATE TABLE IF NOT EXISTS func.t_medical_expense
(
    medical_expense_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id              BIGINT,
    provider_id                 BIGINT,
    family_member_id            BIGINT,
    service_date                DATE NOT NULL,
    service_description         TEXT,
    procedure_code              TEXT,
    diagnosis_code              TEXT,
    billed_amount               NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    insurance_discount          NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    insurance_paid              NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    patient_responsibility      NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    paid_amount                 NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    paid_date                   DATE,
    is_out_of_network           BOOLEAN DEFAULT FALSE NOT NULL,
    claim_number                TEXT,
    claim_status                TEXT DEFAULT 'submitted' NOT NULL,
    owner                       TEXT NOT NULL,
    active_status               BOOLEAN DEFAULT TRUE NOT NULL,
    date_added                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_updated                TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_medical_expense_transaction FOREIGN KEY (owner, transaction_id)
        REFERENCES func.t_transaction(owner, transaction_id) ON DELETE CASCADE,
    CONSTRAINT fk_medical_expense_provider FOREIGN KEY (provider_id)
        REFERENCES func.t_medical_provider(provider_id),
    CONSTRAINT fk_medical_expense_family_member FOREIGN KEY (owner, family_member_id)
        REFERENCES func.t_family_member (owner, family_member_id) ON UPDATE CASCADE,
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
    CONSTRAINT ck_medical_expense_financial_consistency CHECK (
        billed_amount >= (insurance_discount + insurance_paid + patient_responsibility)
    )
);

CREATE INDEX IF NOT EXISTS idx_medical_expense_provider ON func.t_medical_expense(provider_id);
CREATE INDEX IF NOT EXISTS idx_medical_expense_family_member ON func.t_medical_expense(family_member_id);
CREATE INDEX IF NOT EXISTS idx_medical_expense_service_date ON func.t_medical_expense(service_date);
CREATE INDEX IF NOT EXISTS idx_medical_expense_claim_status ON func.t_medical_expense(claim_status);
CREATE INDEX IF NOT EXISTS idx_medical_expense_active ON func.t_medical_expense(active_status, service_date);
CREATE INDEX IF NOT EXISTS idx_medical_expense_owner ON func.t_medical_expense(owner);
