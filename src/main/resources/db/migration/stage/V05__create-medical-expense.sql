-- Medical Expense Table Creation for Stage Environment
-- Links medical expenses to existing transactions with 1:1 relationship
-- Supports comprehensive medical expense tracking with family member support

CREATE TABLE IF NOT EXISTS public.t_medical_expense (
    medical_expense_id          BIGSERIAL PRIMARY KEY,
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
    date_added                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_updated               TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT fk_medical_expense_transaction FOREIGN KEY (transaction_id)
        REFERENCES public.t_transaction(transaction_id) ON DELETE CASCADE,
    CONSTRAINT fk_medical_expense_provider FOREIGN KEY (provider_id)
        REFERENCES public.t_medical_provider(provider_id),
    CONSTRAINT fk_medical_expense_family_member FOREIGN KEY (family_member_id)
        REFERENCES public.t_family_member(family_member_id),
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
CREATE UNIQUE INDEX idx_medical_expense_transaction ON public.t_medical_expense(transaction_id);
CREATE INDEX idx_medical_expense_provider ON public.t_medical_expense(provider_id);
CREATE INDEX idx_medical_expense_family_member ON public.t_medical_expense(family_member_id);
CREATE INDEX idx_medical_expense_service_date ON public.t_medical_expense(service_date);
CREATE INDEX idx_medical_expense_claim_number ON public.t_medical_expense(claim_number) WHERE claim_number IS NOT NULL;
CREATE INDEX idx_medical_expense_claim_status ON public.t_medical_expense(claim_status);
CREATE INDEX idx_medical_expense_active ON public.t_medical_expense(active_status, service_date);

-- Comments for documentation
COMMENT ON TABLE public.t_medical_expense IS 'Medical expenses linked to transactions with comprehensive tracking';
COMMENT ON COLUMN public.t_medical_expense.medical_expense_id IS 'Primary key for medical expense records';
COMMENT ON COLUMN public.t_medical_expense.transaction_id IS 'Foreign key to t_transaction (1:1 relationship)';
COMMENT ON COLUMN public.t_medical_expense.provider_id IS 'Foreign key to t_medical_provider';
COMMENT ON COLUMN public.t_medical_expense.family_member_id IS 'Foreign key to t_family_member for tracking which family member';
COMMENT ON COLUMN public.t_medical_expense.service_date IS 'Date medical service was provided (different from payment date)';
COMMENT ON COLUMN public.t_medical_expense.billed_amount IS 'Original amount billed by provider';
COMMENT ON COLUMN public.t_medical_expense.insurance_discount IS 'Insurance negotiated discount amount';
COMMENT ON COLUMN public.t_medical_expense.insurance_paid IS 'Amount paid by insurance';
COMMENT ON COLUMN public.t_medical_expense.patient_responsibility IS 'Amount patient is responsible to pay';
COMMENT ON COLUMN public.t_medical_expense.is_out_of_network IS 'Whether provider is out of insurance network';
COMMENT ON COLUMN public.t_medical_expense.claim_status IS 'Status of insurance claim processing';