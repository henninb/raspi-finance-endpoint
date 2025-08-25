-- V03: Create Medical Provider table (Integration Test)
-- Medical provider information for healthcare expense tracking

CREATE TABLE IF NOT EXISTS int.t_medical_provider (
    provider_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_name       TEXT NOT NULL,
    provider_type       TEXT NOT NULL DEFAULT 'general',
    specialty           TEXT,
    npi                 TEXT UNIQUE, -- National Provider Identifier
    tax_id              TEXT, -- Tax ID/EIN for business providers

    -- Address information
    address_line1       TEXT,
    address_line2       TEXT,
    city               TEXT,
    state              TEXT,
    zip_code           TEXT,
    country            TEXT DEFAULT 'US',

    -- Contact information
    phone              TEXT,
    fax                TEXT,
    email              TEXT,
    website            TEXT,

    -- Provider details
    network_status     TEXT DEFAULT 'unknown', -- in_network, out_of_network, unknown
    billing_name       TEXT, -- Name used for billing/claims
    notes              TEXT,

    -- Audit and status fields
    active_status      BOOLEAN DEFAULT TRUE NOT NULL,
    date_added         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_updated       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
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
    CONSTRAINT ck_npi_format CHECK (npi IS NULL OR (LENGTH(npi) = 10)),
    CONSTRAINT ck_zip_code_format CHECK (zip_code IS NULL OR (LENGTH(zip_code) >= 5)),
    CONSTRAINT ck_phone_format CHECK (phone IS NULL OR (length(phone) >= 10))
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_medical_provider_name ON int.t_medical_provider(provider_name);
CREATE INDEX IF NOT EXISTS idx_medical_provider_type ON int.t_medical_provider(provider_type);
CREATE INDEX IF NOT EXISTS idx_medical_provider_specialty ON int.t_medical_provider(specialty);
CREATE INDEX IF NOT EXISTS idx_medical_provider_npi ON int.t_medical_provider(npi);
CREATE INDEX IF NOT EXISTS idx_medical_provider_active ON int.t_medical_provider(active_status, provider_name);
CREATE INDEX IF NOT EXISTS idx_medical_provider_network ON int.t_medical_provider(network_status, provider_type);
CREATE INDEX IF NOT EXISTS idx_medical_provider_location ON int.t_medical_provider(state, city);

-- Insert common medical provider types for initial data
INSERT INTO int.t_medical_provider (provider_name, provider_type, specialty, network_status) VALUES
('int_test_provider', 'general', NULL, 'in_network'),
('int_test_pharmacy', 'pharmacy', 'retail_pharmacy', 'in_network'),
('int_test_urgent_care', 'urgent_care', NULL, 'out_of_network'),
('int_test_hospital', 'hospital', NULL, 'in_network'),
('int_test_laboratory', 'laboratory', 'general_lab', 'in_network');