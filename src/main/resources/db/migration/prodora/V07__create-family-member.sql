-- V07: Create Family Member table (Production Oracle)
-- Family member tracking for medical expense attribution

CREATE SEQUENCE prodora.t_family_member_family_member_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE prodora.t_family_member (
    family_member_id    NUMBER(19) DEFAULT prodora.t_family_member_family_member_id_seq.NEXTVAL PRIMARY KEY,
    owner               VARCHAR2(100) NOT NULL,
    member_name         VARCHAR2(100) NOT NULL,
    relationship        VARCHAR2(20) DEFAULT 'self' NOT NULL,
    date_of_birth       DATE,
    insurance_member_id VARCHAR2(50),

    -- Medical identifiers
    ssn_last_four      VARCHAR2(4),
    medical_record_number VARCHAR2(50),

    -- Audit and status fields
    active_status      NUMBER(1) DEFAULT 1 NOT NULL CHECK (active_status IN (0, 1)),
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
    CONSTRAINT ck_ssn_last_four_format CHECK (ssn_last_four IS NULL OR REGEXP_LIKE(ssn_last_four, '^[0-9]{4}$')),
    CONSTRAINT ck_insurance_member_id_length CHECK (insurance_member_id IS NULL OR length(insurance_member_id) <= 50),
    CONSTRAINT ck_medical_record_number_length CHECK (medical_record_number IS NULL OR length(medical_record_number) <= 50),
    CONSTRAINT uk_family_member_owner_name UNIQUE (owner, member_name)
);

-- Indexes for performance
CREATE INDEX idx_family_member_owner ON prodora.t_family_member(owner);
CREATE INDEX idx_family_member_relationship ON prodora.t_family_member(owner, relationship);
CREATE INDEX idx_family_member_active ON prodora.t_family_member(active_status, owner);
CREATE INDEX idx_family_member_insurance ON prodora.t_family_member(insurance_member_id);

-- Insert default family member for existing owners (self)
INSERT INTO prodora.t_family_member (owner, member_name, relationship)
SELECT DISTINCT account_name_owner, account_name_owner, 'self'
FROM prodora.t_account
WHERE active_status = 1
AND account_name_owner NOT IN (
    SELECT owner FROM prodora.t_family_member WHERE relationship = 'self'
);