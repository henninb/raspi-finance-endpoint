-- V05: Create Family Member table (Oracle Test)
-- Family member tracking for medical expense attribution

CREATE SEQUENCE ora.t_family_member_family_member_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE ora.t_family_member (
    family_member_id    NUMBER(19) DEFAULT ora.t_family_member_family_member_id_seq.NEXTVAL PRIMARY KEY,
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
    CONSTRAINT ck_family_member_name_not_empty CHECK (length(trim(member_name)) > 0),
    CONSTRAINT ck_family_owner_not_empty CHECK (length(trim(owner)) > 0),
    CONSTRAINT ck_ssn_last_four_format CHECK (ssn_last_four IS NULL OR REGEXP_LIKE(ssn_last_four, '^[0-9]{4}$')),
    CONSTRAINT ck_insurance_member_id_length CHECK (insurance_member_id IS NULL OR length(insurance_member_id) <= 50),
    CONSTRAINT ck_medical_record_number_length CHECK (medical_record_number IS NULL OR length(medical_record_number) <= 50),
    CONSTRAINT uk_family_member_owner_name UNIQUE (owner, member_name)
);

-- Indexes for performance
CREATE INDEX idx_family_member_owner ON ora.t_family_member(owner);
CREATE INDEX idx_family_member_relationship ON ora.t_family_member(owner, relationship);
CREATE INDEX idx_family_member_active ON ora.t_family_member(active_status, owner);
CREATE INDEX idx_family_member_insurance ON ora.t_family_member(insurance_member_id);

-- Insert oracle test family members
INSERT INTO ora.t_family_member (owner, member_name, relationship, insurance_member_id) VALUES
('ora_test_user', 'ora_test_user', 'self', 'ORA001');
INSERT INTO ora.t_family_member (owner, member_name, relationship, insurance_member_id) VALUES
('ora_test_user', 'ora_spouse', 'spouse', 'ORA002');
INSERT INTO ora.t_family_member (owner, member_name, relationship, insurance_member_id) VALUES
('ora_test_user', 'ora_child1', 'child', 'ORA003');
INSERT INTO ora.t_family_member (owner, member_name, relationship, insurance_member_id) VALUES
('ora_test_user', 'ora_child2', 'child', 'ORA004');
INSERT INTO ora.t_family_member (owner, member_name, relationship, insurance_member_id) VALUES
('ora_test_user', 'ora_dependent', 'dependent', 'ORA005');