-- V09: Create Family Member table
-- Family member tracking for medical expense attribution

CREATE TABLE IF NOT EXISTS public.t_family_member (
    family_member_id    BIGSERIAL PRIMARY KEY,
    owner               TEXT NOT NULL,
    member_name         TEXT NOT NULL,
    relationship        TEXT NOT NULL DEFAULT 'self',
    date_of_birth       DATE,
    insurance_member_id TEXT,

    -- Medical identifiers
    ssn_last_four      TEXT,
    medical_record_number TEXT,

    -- Audit and status fields
    active_status      BOOLEAN DEFAULT TRUE NOT NULL,
    date_added         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_updated       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
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

-- Unique constraint for owner + member_name combination
ALTER TABLE public.t_family_member
ADD CONSTRAINT uk_family_member_owner_name UNIQUE (owner, member_name);

-- Indexes for performance
CREATE INDEX idx_family_member_owner ON public.t_family_member(owner);
CREATE INDEX idx_family_member_relationship ON public.t_family_member(owner, relationship);
CREATE INDEX idx_family_member_active ON public.t_family_member(active_status, owner) WHERE active_status = true;
CREATE INDEX idx_family_member_insurance ON public.t_family_member(insurance_member_id) WHERE insurance_member_id IS NOT NULL;

-- Insert default family member for existing owners (self)
-- This ensures existing medical expenses can be attributed to the primary account holder
INSERT INTO public.t_family_member (owner, member_name, relationship)
SELECT DISTINCT account_name_owner, account_name_owner, 'self'
FROM public.t_account
WHERE active_status = true
AND account_name_owner NOT IN (
    SELECT owner FROM public.t_family_member WHERE relationship = 'self'
);