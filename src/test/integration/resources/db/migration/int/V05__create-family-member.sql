-- V05: Create Family Member table (Integration Test)
-- Family member tracking for medical expense attribution

CREATE TABLE IF NOT EXISTS int.t_family_member (
    family_member_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    CONSTRAINT ck_family_member_name_not_empty CHECK (length(trim(member_name)) > 0),
    CONSTRAINT ck_family_owner_not_empty CHECK (length(trim(owner)) > 0),
    CONSTRAINT ck_ssn_last_four_format CHECK (ssn_last_four IS NULL OR (LENGTH(ssn_last_four) = 4)),
    CONSTRAINT ck_insurance_member_id_length CHECK (insurance_member_id IS NULL OR length(insurance_member_id) <= 50),
    CONSTRAINT ck_medical_record_number_length CHECK (medical_record_number IS NULL OR length(medical_record_number) <= 50)
);

-- Unique constraint for owner + member_name combination  
ALTER TABLE int.t_family_member 
ADD CONSTRAINT uk_family_member_owner_name UNIQUE (owner, member_name);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_family_member_owner ON int.t_family_member(owner);
CREATE INDEX IF NOT EXISTS idx_family_member_relationship ON int.t_family_member(owner, relationship);
CREATE INDEX IF NOT EXISTS idx_family_member_active ON int.t_family_member(active_status, owner);
CREATE INDEX IF NOT EXISTS idx_family_member_insurance ON int.t_family_member(insurance_member_id);

-- Insert integration test family members
INSERT INTO int.t_family_member (owner, member_name, relationship, insurance_member_id) VALUES
('int_test_user', 'int_test_user', 'self', 'INT001'),
('int_test_user', 'int_spouse', 'spouse', 'INT002'),
('int_test_user', 'int_child1', 'child', 'INT003'),
('int_test_user', 'int_child2', 'child', 'INT004'),
('int_test_user', 'int_dependent', 'dependent', 'INT005');