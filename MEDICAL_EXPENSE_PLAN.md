# Medical Expense Tracking Feature Implementation Plan

## Overview
This document outlines a comprehensive plan to add medical expense tracking capabilities to the raspi-finance-endpoint application. The implementation will leverage the existing transaction system while extending it with medical-specific functionality.

## Executive Summary
- **Approach**: Extend existing transaction system rather than create parallel system
- **Strategy**: Incremental, non-breaking changes using Flyway migrations
- **Architecture**: Leverage current JPA entities with proper relationships
- **Data Integrity**: Maintain all existing constraints and foreign key relationships

## Current System Analysis

### Existing Architecture Strengths
- **Robust Transaction System**: Existing `t_transaction` table with proper validation
- **Account Management**: `t_account` with account types and proper constraints
- **Category System**: `t_category` table for transaction classification
- **Audit Trail**: All entities have `date_added` and `date_updated` fields
- **Receipt Management**: `t_receipt_image` for document storage
- **Validation Framework**: Comprehensive validation with Jakarta validation

### Current Database Schema (Relevant Tables)
```sql
t_account (account_id, account_name_owner, account_type, ...)
t_transaction (transaction_id, account_id, description, amount, category, ...)
t_category (category_id, category_name, ...)
t_receipt_image (receipt_image_id, transaction_id, image, ...)
```

## Medical Expense Requirements Analysis

### Core Data Points Required
1. **Service Information**
   - Date of service (different from transaction/payment date)
   - Provider information
   - Medical procedure/service type

2. **Financial Details**
   - Billed amount (original amount charged)
   - Insurance discount/adjustment
   - Insurance paid amount
   - Patient responsibility
   - Paid date (when patient actually paid)

3. **Insurance Tracking**
   - Insurance plan information
   - Claim status
   - Out-of-network vs in-network
   - Deductible tracking
   - Out-of-pocket maximum tracking

4. **Medical Categorization**
   - Service categories (preventive, diagnostic, treatment, etc.)
   - Provider specialties

## Proposed Solution Architecture

### Design Principles
1. **Extend, Don't Replace**: Build on existing transaction system
2. **Non-Breaking Changes**: All changes are additive
3. **Data Integrity**: Maintain all existing relationships and constraints
4. **Audit Trail**: Full audit trail for all medical data
5. **Type Safety**: Strong typing with proper validation

### Entity Relationship Design

```
Account (existing)
├── AccountType: Add medical account types (HSA, FSA, Medical)
└── Transactions (existing)
    └── MedicalExpense (new 1:1 relationship)
        ├── MedicalProvider (new M:1 relationship)
        ├── InsurancePlan (new M:1 relationship)
        └── MedicalServiceCategory (new M:1 relationship)
```

## Implementation Phases

### Phase 1: Account Type Extensions and Provider Management
**Duration**: 1-2 days
**Risk**: Low
**Dependencies**: None

#### 1.1 Extend AccountType Enum (V07__extend-account-types-medical.sql)
```sql
-- Add new account types for medical accounts
ALTER TABLE public.t_account 
DROP CONSTRAINT IF EXISTS ck_account_type;

ALTER TABLE public.t_account 
ADD CONSTRAINT ck_account_type 
CHECK (account_type IN ('debit', 'credit', 'undefined', 'hsa', 'fsa', 'medical_savings'));
```

#### 1.2 Create Medical Provider Entity (V08__create-medical-provider.sql)
```sql
CREATE TABLE IF NOT EXISTS public.t_medical_provider (
    provider_id         BIGSERIAL PRIMARY KEY,
    provider_name       TEXT NOT NULL,
    provider_type       TEXT NOT NULL DEFAULT 'general', -- general, specialist, hospital, pharmacy, lab
    specialty           TEXT,
    npi                 TEXT UNIQUE, -- National Provider Identifier
    address_line1       TEXT,
    address_line2       TEXT,
    city               TEXT,
    state              TEXT,
    zip_code           TEXT,
    phone              TEXT,
    active_status      BOOLEAN DEFAULT TRUE NOT NULL,
    date_added         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_updated       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT ck_provider_type CHECK (provider_type IN (
        'general', 'specialist', 'hospital', 'pharmacy', 'laboratory', 'imaging', 'other'
    )),
    CONSTRAINT ck_provider_name_lowercase CHECK (provider_name = lower(provider_name))
);

CREATE INDEX idx_medical_provider_name ON public.t_medical_provider(provider_name);
CREATE INDEX idx_medical_provider_type ON public.t_medical_provider(provider_type);
```

#### 1.3 Domain Entity Updates
```kotlin
// AccountType.kt - Add new enum values
enum class AccountType(val label: String) {
    @JsonProperty("credit") Credit("credit"),
    @JsonProperty("debit") Debit("debit"),
    @JsonProperty("hsa") HSA("hsa"),
    @JsonProperty("fsa") FSA("fsa"),
    @JsonProperty("medical_savings") MedicalSavings("medical_savings"),
    @JsonProperty("undefined") Undefined("undefined");
}

// New MedicalProvider.kt
@Entity
@Table(name = "t_medical_provider")
data class MedicalProvider(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var providerId: Long,
    
    @Column(name = "provider_name", nullable = false)
    @Convert(converter = LowerCaseConverter::class)
    var providerName: String,
    
    @Column(name = "provider_type", nullable = false)
    @Convert(converter = MedicalProviderTypeConverter::class)
    var providerType: MedicalProviderType,
    
    // Additional fields...
)
```

### Phase 2: Core Medical Expense Entity
**Duration**: 2-3 days
**Risk**: Medium
**Dependencies**: Phase 1

#### 2.1 Create Medical Expense Entity (V09__create-medical-expense.sql)
```sql
CREATE TABLE IF NOT EXISTS public.t_medical_expense (
    medical_expense_id          BIGSERIAL PRIMARY KEY,
    transaction_id              BIGINT NOT NULL,
    provider_id                 BIGINT,
    
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
    claim_status               TEXT DEFAULT 'submitted',
    
    -- Audit fields
    active_status              BOOLEAN DEFAULT TRUE NOT NULL,
    date_added                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_updated               TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Constraints
    CONSTRAINT fk_medical_transaction FOREIGN KEY (transaction_id) 
        REFERENCES public.t_transaction(transaction_id) ON DELETE CASCADE,
    CONSTRAINT fk_medical_provider FOREIGN KEY (provider_id) 
        REFERENCES public.t_medical_provider(provider_id),
    CONSTRAINT ck_claim_status CHECK (claim_status IN (
        'submitted', 'processing', 'approved', 'denied', 'paid', 'closed'
    )),
    CONSTRAINT ck_financial_amounts CHECK (
        billed_amount >= 0 AND 
        insurance_discount >= 0 AND 
        insurance_paid >= 0 AND 
        patient_responsibility >= 0
    ),
    CONSTRAINT ck_service_date_valid CHECK (service_date <= CURRENT_DATE)
);

CREATE UNIQUE INDEX idx_medical_expense_transaction ON public.t_medical_expense(transaction_id);
CREATE INDEX idx_medical_expense_provider ON public.t_medical_expense(provider_id);
CREATE INDEX idx_medical_expense_service_date ON public.t_medical_expense(service_date);
CREATE INDEX idx_medical_expense_claim ON public.t_medical_expense(claim_number) WHERE claim_number IS NOT NULL;
```

#### 2.2 Medical Expense Domain Entity
```kotlin
@Entity
@Table(name = "t_medical_expense")
data class MedicalExpense(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medical_expense_id")
    var medicalExpenseId: Long,

    @Column(name = "transaction_id", nullable = false)
    var transactionId: Long,

    @Column(name = "service_date", nullable = false)
    @ValidDate
    var serviceDate: Date,

    @Column(name = "service_description")
    @Size(max = 200)
    var serviceDescription: String?,

    @Column(name = "billed_amount", precision = 12, scale = 2)
    @Digits(integer = 12, fraction = 2)
    var billedAmount: BigDecimal,

    @Column(name = "insurance_discount", precision = 12, scale = 2)
    @Digits(integer = 12, fraction = 2)
    var insuranceDiscount: BigDecimal,

    @Column(name = "insurance_paid", precision = 12, scale = 2)
    @Digits(integer = 12, fraction = 2)
    var insurancePaid: BigDecimal,

    @Column(name = "patient_responsibility", precision = 12, scale = 2)
    @Digits(integer = 12, fraction = 2)
    var patientResponsibility: BigDecimal,

    @Column(name = "is_out_of_network")
    var isOutOfNetwork: Boolean = false,

    @Column(name = "claim_status")
    @Convert(converter = ClaimStatusConverter::class)
    var claimStatus: ClaimStatus = ClaimStatus.Submitted,

    // Relationships
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", insertable = false, updatable = false)
    var transaction: Transaction?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", insertable = false, updatable = false)
    var provider: MedicalProvider?
) {
    // Calculated fields
    @Transient
    fun getNetAmount(): BigDecimal = billedAmount - insuranceDiscount - insurancePaid
    
    @Transient
    fun isFullyPaid(): Boolean = patientResponsibility == BigDecimal.ZERO || paidDate != null
}
```

### Phase 3: Insurance Plan Management
**Duration**: 2-3 days
**Risk**: Medium
**Dependencies**: Phase 2

#### 3.1 Insurance Plan Entity (V10__create-insurance-plan.sql)
```sql
CREATE TABLE IF NOT EXISTS public.t_insurance_plan (
    insurance_plan_id       BIGSERIAL PRIMARY KEY,
    plan_name              TEXT NOT NULL,
    insurance_company      TEXT NOT NULL,
    policy_number          TEXT,
    group_number           TEXT,
    
    -- Plan details
    plan_year              INTEGER NOT NULL,
    deductible_individual  NUMERIC(12,2) DEFAULT 0.00,
    deductible_family      NUMERIC(12,2) DEFAULT 0.00,
    out_of_pocket_max_individual NUMERIC(12,2) DEFAULT 0.00,
    out_of_pocket_max_family     NUMERIC(12,2) DEFAULT 0.00,
    
    -- Coverage details
    copay_primary_care     NUMERIC(12,2) DEFAULT 0.00,
    copay_specialist       NUMERIC(12,2) DEFAULT 0.00,
    coinsurance_in_network NUMERIC(5,2) DEFAULT 0.00, -- percentage
    coinsurance_out_network NUMERIC(5,2) DEFAULT 0.00, -- percentage
    
    -- Status
    effective_date         DATE NOT NULL,
    expiration_date        DATE,
    active_status          BOOLEAN DEFAULT TRUE NOT NULL,
    date_added             TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_updated           TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT ck_plan_year_valid CHECK (plan_year BETWEEN 2000 AND 2100),
    CONSTRAINT ck_deductible_valid CHECK (
        deductible_individual >= 0 AND deductible_family >= 0
    ),
    CONSTRAINT ck_out_of_pocket_valid CHECK (
        out_of_pocket_max_individual >= 0 AND out_of_pocket_max_family >= 0
    ),
    CONSTRAINT ck_coinsurance_percentage CHECK (
        coinsurance_in_network BETWEEN 0 AND 100 AND 
        coinsurance_out_network BETWEEN 0 AND 100
    )
);

CREATE INDEX idx_insurance_plan_year ON public.t_insurance_plan(plan_year);
CREATE INDEX idx_insurance_plan_active ON public.t_insurance_plan(active_status, effective_date);
```

#### 3.2 Link Medical Expenses to Insurance Plans (V11__link-medical-expense-insurance.sql)
```sql
ALTER TABLE public.t_medical_expense 
ADD COLUMN insurance_plan_id BIGINT;

ALTER TABLE public.t_medical_expense 
ADD CONSTRAINT fk_medical_expense_insurance_plan 
FOREIGN KEY (insurance_plan_id) REFERENCES public.t_insurance_plan(insurance_plan_id);

CREATE INDEX idx_medical_expense_insurance ON public.t_medical_expense(insurance_plan_id);
```

### Phase 4: Deductible and Out-of-Pocket Tracking
**Duration**: 3-4 days
**Risk**: High
**Dependencies**: Phase 3

#### 4.1 Annual Medical Totals Tracking (V12__create-medical-totals.sql)
```sql
CREATE TABLE IF NOT EXISTS public.t_medical_annual_totals (
    totals_id              BIGSERIAL PRIMARY KEY,
    insurance_plan_id      BIGINT NOT NULL,
    plan_year              INTEGER NOT NULL,
    
    -- Running totals
    total_billed           NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    total_insurance_paid   NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    total_patient_paid     NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    
    -- Deductible tracking
    deductible_met_individual    NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    deductible_met_family       NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    deductible_remaining_individual NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    deductible_remaining_family     NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    
    -- Out-of-pocket tracking
    out_of_pocket_spent_individual NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    out_of_pocket_spent_family     NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    out_of_pocket_remaining_individual NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    out_of_pocket_remaining_family     NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    
    -- Last calculation date for performance
    last_calculated        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Audit fields
    date_added             TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_updated           TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_medical_totals_insurance_plan 
        FOREIGN KEY (insurance_plan_id) REFERENCES public.t_insurance_plan(insurance_plan_id),
    CONSTRAINT uk_medical_totals_plan_year 
        UNIQUE (insurance_plan_id, plan_year),
    CONSTRAINT ck_totals_non_negative CHECK (
        total_billed >= 0 AND 
        total_insurance_paid >= 0 AND 
        total_patient_paid >= 0 AND
        deductible_met_individual >= 0 AND
        deductible_met_family >= 0 AND
        out_of_pocket_spent_individual >= 0 AND
        out_of_pocket_spent_family >= 0
    )
);

CREATE INDEX idx_medical_totals_plan_year ON public.t_medical_annual_totals(insurance_plan_id, plan_year);
```

#### 4.2 Stored Procedures for Total Calculations (V13__medical-totals-functions.sql)
```sql
-- Function to recalculate medical totals for a given plan year
CREATE OR REPLACE FUNCTION fn_recalculate_medical_totals(
    p_insurance_plan_id BIGINT,
    p_plan_year INTEGER
)
RETURNS VOID
SET SCHEMA 'public'
LANGUAGE PLPGSQL
AS $$
DECLARE
    v_plan_deductible_individual NUMERIC(12,2);
    v_plan_deductible_family NUMERIC(12,2);
    v_plan_oop_max_individual NUMERIC(12,2);
    v_plan_oop_max_family NUMERIC(12,2);
    v_total_billed NUMERIC(12,2) := 0.00;
    v_total_insurance_paid NUMERIC(12,2) := 0.00;
    v_total_patient_paid NUMERIC(12,2) := 0.00;
BEGIN
    -- Get plan limits
    SELECT deductible_individual, deductible_family, 
           out_of_pocket_max_individual, out_of_pocket_max_family
    INTO v_plan_deductible_individual, v_plan_deductible_family,
         v_plan_oop_max_individual, v_plan_oop_max_family
    FROM t_insurance_plan 
    WHERE insurance_plan_id = p_insurance_plan_id;
    
    -- Calculate totals from medical expenses
    SELECT COALESCE(SUM(me.billed_amount), 0),
           COALESCE(SUM(me.insurance_paid), 0),
           COALESCE(SUM(me.patient_responsibility), 0)
    INTO v_total_billed, v_total_insurance_paid, v_total_patient_paid
    FROM t_medical_expense me
    JOIN t_transaction t ON me.transaction_id = t.transaction_id
    WHERE me.insurance_plan_id = p_insurance_plan_id
    AND EXTRACT(YEAR FROM me.service_date) = p_plan_year
    AND me.active_status = true;
    
    -- Upsert totals record
    INSERT INTO t_medical_annual_totals (
        insurance_plan_id, plan_year, total_billed, total_insurance_paid, 
        total_patient_paid, deductible_met_individual, deductible_remaining_individual,
        out_of_pocket_spent_individual, out_of_pocket_remaining_individual,
        last_calculated
    ) VALUES (
        p_insurance_plan_id, p_plan_year, v_total_billed, v_total_insurance_paid,
        v_total_patient_paid,
        LEAST(v_total_patient_paid, v_plan_deductible_individual),
        GREATEST(0, v_plan_deductible_individual - v_total_patient_paid),
        v_total_patient_paid,
        GREATEST(0, v_plan_oop_max_individual - v_total_patient_paid),
        CURRENT_TIMESTAMP
    )
    ON CONFLICT (insurance_plan_id, plan_year)
    DO UPDATE SET
        total_billed = EXCLUDED.total_billed,
        total_insurance_paid = EXCLUDED.total_insurance_paid,
        total_patient_paid = EXCLUDED.total_patient_paid,
        deductible_met_individual = EXCLUDED.deductible_met_individual,
        deductible_remaining_individual = EXCLUDED.deductible_remaining_individual,
        out_of_pocket_spent_individual = EXCLUDED.out_of_pocket_spent_individual,
        out_of_pocket_remaining_individual = EXCLUDED.out_of_pocket_remaining_individual,
        last_calculated = CURRENT_TIMESTAMP,
        date_updated = CURRENT_TIMESTAMP;
END;
$$;

-- Trigger to automatically recalculate totals when medical expenses change
CREATE OR REPLACE FUNCTION fn_trigger_recalc_medical_totals()
RETURNS TRIGGER
SET SCHEMA 'public'
LANGUAGE PLPGSQL
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM fn_recalculate_medical_totals(
            OLD.insurance_plan_id, 
            EXTRACT(YEAR FROM OLD.service_date)::INTEGER
        );
        RETURN OLD;
    ELSE
        PERFORM fn_recalculate_medical_totals(
            NEW.insurance_plan_id, 
            EXTRACT(YEAR FROM NEW.service_date)::INTEGER
        );
        RETURN NEW;
    END IF;
END;
$$;

CREATE TRIGGER tr_medical_expense_totals_update
    AFTER INSERT OR UPDATE OR DELETE ON t_medical_expense
    FOR EACH ROW
    WHEN (NEW.insurance_plan_id IS NOT NULL OR OLD.insurance_plan_id IS NOT NULL)
    EXECUTE FUNCTION fn_trigger_recalc_medical_totals();
```

### Phase 5: Medical Categories and Service Types
**Duration**: 1-2 days
**Risk**: Low
**Dependencies**: Phase 2

#### 5.1 Medical Service Categories (V14__create-medical-service-categories.sql)
```sql
CREATE TABLE IF NOT EXISTS public.t_medical_service_category (
    service_category_id    BIGSERIAL PRIMARY KEY,
    category_name          TEXT NOT NULL UNIQUE,
    category_type          TEXT NOT NULL DEFAULT 'service',
    description           TEXT,
    is_preventive         BOOLEAN DEFAULT FALSE,
    typical_copay         NUMERIC(12,2) DEFAULT 0.00,
    
    active_status         BOOLEAN DEFAULT TRUE NOT NULL,
    date_added            TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_updated          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT ck_service_category_type CHECK (category_type IN (
        'service', 'medication', 'equipment', 'procedure', 'diagnostic', 'other'
    )),
    CONSTRAINT ck_service_category_name_lowercase CHECK (category_name = lower(category_name))
);

-- Insert common medical service categories
INSERT INTO t_medical_service_category (category_name, category_type, is_preventive) VALUES
('annual_physical', 'service', true),
('specialist_visit', 'service', false),
('urgent_care', 'service', false),
('emergency_room', 'service', false),
('prescription_medication', 'medication', false),
('laboratory_tests', 'diagnostic', false),
('imaging_xray', 'diagnostic', false),
('imaging_mri', 'diagnostic', false),
('imaging_ct_scan', 'diagnostic', false),
('dental_cleaning', 'service', true),
('dental_procedure', 'service', false),
('vision_exam', 'service', true),
('physical_therapy', 'service', false),
('mental_health', 'service', false);
```

#### 5.2 Link Medical Expenses to Service Categories (V15__link-medical-service-categories.sql)
```sql
ALTER TABLE public.t_medical_expense 
ADD COLUMN service_category_id BIGINT;

ALTER TABLE public.t_medical_expense 
ADD CONSTRAINT fk_medical_expense_service_category 
FOREIGN KEY (service_category_id) REFERENCES public.t_medical_service_category(service_category_id);

CREATE INDEX idx_medical_expense_service_category ON public.t_medical_expense(service_category_id);
```

## Service Layer Implementation

### Medical Expense Service Interface
```kotlin
interface MedicalExpenseService {
    fun createMedicalExpense(medicalExpense: MedicalExpense): MedicalExpense
    fun updateMedicalExpense(medicalExpense: MedicalExpense): MedicalExpense
    fun findMedicalExpensesByAccount(accountId: Long): List<MedicalExpense>
    fun findMedicalExpensesByDateRange(startDate: Date, endDate: Date): List<MedicalExpense>
    fun getMedicalTotalsByPlanYear(insurancePlanId: Long, planYear: Int): MedicalAnnualTotals?
    fun recalculateMedicalTotals(insurancePlanId: Long, planYear: Int)
    fun findOutstandingClaims(): List<MedicalExpense>
}
```

### Repository Layer
```kotlin
interface MedicalExpenseRepository : JpaRepository<MedicalExpense, Long> {
    fun findByTransactionId(transactionId: Long): MedicalExpense?
    fun findByServiceDateBetween(startDate: Date, endDate: Date): List<MedicalExpense>
    fun findByInsurancePlanIdAndServiceDateBetween(
        insurancePlanId: Long, 
        startDate: Date, 
        endDate: Date
    ): List<MedicalExpense>
    fun findByClaimStatus(claimStatus: ClaimStatus): List<MedicalExpense>
}
```

## Controller Layer Implementation

### Medical Expense REST Controller
```kotlin
@RestController
@RequestMapping("/medical-expenses")
@Validated
class MedicalExpenseController(
    private val medicalExpenseService: MedicalExpenseService
) {
    
    @PostMapping
    fun createMedicalExpense(@Valid @RequestBody medicalExpense: MedicalExpense): ResponseEntity<MedicalExpense> {
        val created = medicalExpenseService.createMedicalExpense(medicalExpense)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
    
    @GetMapping("/account/{accountId}")
    fun getMedicalExpensesByAccount(@PathVariable accountId: Long): List<MedicalExpense> {
        return medicalExpenseService.findMedicalExpensesByAccount(accountId)
    }
    
    @GetMapping("/totals/{insurancePlanId}/{planYear}")
    fun getMedicalTotals(
        @PathVariable insurancePlanId: Long,
        @PathVariable planYear: Int
    ): ResponseEntity<MedicalAnnualTotals> {
        val totals = medicalExpenseService.getMedicalTotalsByPlanYear(insurancePlanId, planYear)
        return if (totals != null) {
            ResponseEntity.ok(totals)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
```

## Testing Strategy

### Unit Tests
- Entity validation tests
- Service layer business logic tests
- Repository query tests
- Controller endpoint tests

### Integration Tests
- Database integration tests with H2
- Transaction rollback tests
- Foreign key constraint tests
- Trigger and stored procedure tests

### Functional Tests
- End-to-end API tests
- Medical expense workflow tests
- Total calculation accuracy tests
- Multi-user scenario tests

### Test Data Builders
```kotlin
class MedicalExpenseTestDataBuilder {
    companion object {
        fun aMedicalExpense(): MedicalExpense {
            return MedicalExpense(
                medicalExpenseId = 0L,
                transactionId = 1L,
                serviceDate = Date.valueOf("2024-01-15"),
                billedAmount = BigDecimal("250.00"),
                insuranceDiscount = BigDecimal("50.00"),
                insurancePaid = BigDecimal("150.00"),
                patientResponsibility = BigDecimal("50.00"),
                isOutOfNetwork = false,
                claimStatus = ClaimStatus.Approved
            )
        }
    }
}
```

## Migration Strategy

### Migration Ordering
1. **V07**: Extend AccountType enum (non-breaking)
2. **V08**: Create MedicalProvider table (additive)
3. **V09**: Create MedicalExpense table (additive)
4. **V10**: Create InsurancePlan table (additive)
5. **V11**: Add insurance_plan_id to MedicalExpense (additive)
6. **V12**: Create MedicalAnnualTotals table (additive)
7. **V13**: Create stored procedures and triggers (additive)
8. **V14**: Create MedicalServiceCategory table (additive)
9. **V15**: Add service_category_id to MedicalExpense (additive)

### Rollback Strategy
Each migration includes rollback scripts:
```sql
-- V07 Rollback
ALTER TABLE public.t_account 
DROP CONSTRAINT ck_account_type;
ALTER TABLE public.t_account 
ADD CONSTRAINT ck_account_type 
CHECK (account_type IN ('debit', 'credit', 'undefined'));
```

## Performance Considerations

### Database Indexes
- Primary key indexes on all tables
- Foreign key indexes on relationship columns
- Composite indexes for common query patterns
- Partial indexes where appropriate (e.g., active records only)

### Query Optimization
- Use proper JOIN strategies for related data
- Implement query result caching for totals
- Use database functions for complex calculations
- Implement pagination for large result sets

### Caching Strategy
```kotlin
@Cacheable("medical-totals")
fun getMedicalTotalsByPlanYear(insurancePlanId: Long, planYear: Int): MedicalAnnualTotals? {
    // Implementation
}

@CacheEvict(value = ["medical-totals"], allEntries = true)
fun recalculateMedicalTotals(insurancePlanId: Long, planYear: Int) {
    // Implementation
}
```

## Security Considerations

### Data Access Control
- Implement proper authentication for medical data access
- Use row-level security for multi-tenant scenarios
- Audit all medical data access and modifications
- Encrypt sensitive medical information at rest

### HIPAA Compliance Considerations
- Implement audit logging for all medical data access
- Ensure proper data retention policies
- Implement secure data backup and recovery
- Consider data anonymization for reporting

## Monitoring and Alerting

### Key Metrics to Monitor
- Medical expense creation/update rates
- Total calculation performance
- Failed insurance claim processing
- Database query performance for medical tables

### Logging Strategy
```kotlin
@Component
class MedicalExpenseAuditLogger {
    private val logger = LoggerFactory.getLogger(MedicalExpenseAuditLogger::class.java)
    
    fun logMedicalExpenseCreation(expense: MedicalExpense, user: String) {
        logger.info("Medical expense created: id=${expense.medicalExpenseId}, " +
                   "amount=${expense.patientResponsibility}, user=$user")
    }
}
```

## Data Migration and Cleanup

### Existing Data Integration
- Identify existing medical-related transactions
- Create migration scripts to convert existing data
- Implement data validation for converted records

### Sample Data Migration Script
```sql
-- Identify potential medical transactions
SELECT transaction_id, description, category, amount
FROM t_transaction 
WHERE category IN ('medical', 'healthcare', 'pharmacy', 'doctor')
   OR description ILIKE '%medical%'
   OR description ILIKE '%doctor%'
   OR description ILIKE '%hospital%';
```

## Maintenance and Operations

### Regular Maintenance Tasks
- Monthly totals recalculation verification
- Quarterly insurance plan updates
- Annual data archival for old plan years
- Performance monitoring and optimization

### Backup and Recovery
- Include medical tables in backup strategy
- Test recovery procedures with medical data
- Implement point-in-time recovery for critical medical data

## Success Metrics

### Functional Metrics
- Successful medical expense creation rate: >99%
- Total calculation accuracy: 100%
- API response times: <200ms for standard queries
- Data integrity: Zero foreign key violations

### Business Metrics
- User adoption of medical expense features
- Reduction in manual medical expense tracking
- Improved accuracy in deductible and out-of-pocket tracking
- Enhanced financial planning capabilities

## Risks and Mitigation

### Technical Risks
1. **Database Performance**: Large medical datasets may impact query performance
   - **Mitigation**: Implement proper indexing and query optimization
   
2. **Data Integrity**: Complex relationships between medical entities
   - **Mitigation**: Comprehensive foreign key constraints and validation

3. **Calculation Accuracy**: Complex deductible and out-of-pocket calculations
   - **Mitigation**: Extensive unit and integration testing

### Business Risks
1. **User Adoption**: Users may not understand new medical features
   - **Mitigation**: Comprehensive documentation and user training

2. **Data Privacy**: Medical data requires enhanced security measures
   - **Mitigation**: Implement proper access controls and audit logging

## Conclusion

This comprehensive plan provides a structured approach to implementing medical expense tracking while maintaining the integrity and performance of the existing system. The incremental approach ensures that each phase can be implemented and tested independently, minimizing risk and allowing for iterative improvements.

The design leverages the existing transaction system's strengths while adding medical-specific capabilities that provide comprehensive tracking of healthcare expenses, insurance interactions, and financial planning tools.