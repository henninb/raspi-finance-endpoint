-- Medical Expense Table Creation for Functional Test Environment
-- Links medical expenses to existing transactions with 1:1 relationship
-- Supports comprehensive medical expense tracking with family member support

CREATE TABLE IF NOT EXISTS func.t_medical_expense (
    medical_expense_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id              BIGINT NOT NULL,
    provider_id                 BIGINT,
    family_member_id            BIGINT,

    -- Core medical expense data
    service_date                DATE NOT NULL,
    service_description         TEXT,
    procedure_code              TEXT, -- CPT/HCPCS codes
    diagnosis_code              TEXT, -- ICD-10 codes

    -- Financial breakdown
    billed_amount              NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    insurance_discount         NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    insurance_paid             NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    patient_responsibility     NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    paid_date                  DATE,

    -- Insurance details
    is_out_of_network          BOOLEAN DEFAULT FALSE NOT NULL,
    claim_number               TEXT,
    claim_status               TEXT DEFAULT 'submitted' NOT NULL,

    -- Audit fields
    active_status              BOOLEAN DEFAULT TRUE NOT NULL,
    date_added                 TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,
    date_updated               TIMESTAMP DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S') NOT NULL,

    -- Constraints
    CONSTRAINT fk_medical_expense_transaction FOREIGN KEY (transaction_id)
        REFERENCES func.t_transaction(transaction_id) ON DELETE CASCADE,
    CONSTRAINT fk_medical_expense_provider FOREIGN KEY (provider_id)
        REFERENCES func.t_medical_provider(provider_id),
    CONSTRAINT fk_medical_expense_family_member FOREIGN KEY (family_member_id)
        REFERENCES func.t_family_member(family_member_id),
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
    CONSTRAINT ck_medical_expense_service_date_valid CHECK (service_date <= CURRENT_DATE),
    CONSTRAINT ck_medical_expense_financial_consistency CHECK (
        billed_amount >= (insurance_discount + insurance_paid + patient_responsibility)
    )
);

-- Performance indexes
CREATE UNIQUE INDEX idx_medical_expense_transaction ON func.t_medical_expense(transaction_id);
CREATE INDEX idx_medical_expense_provider ON func.t_medical_expense(provider_id);
CREATE INDEX idx_medical_expense_family_member ON func.t_medical_expense(family_member_id);
CREATE INDEX idx_medical_expense_service_date ON func.t_medical_expense(service_date);
CREATE INDEX idx_medical_expense_claim_number ON func.t_medical_expense(claim_number);
CREATE INDEX idx_medical_expense_claim_status ON func.t_medical_expense(claim_status);
CREATE INDEX idx_medical_expense_active ON func.t_medical_expense(active_status, service_date);

-- Test data will be created by individual tests using the isolated architecture
-- No shared test data to avoid brittleness and FK constraint issues