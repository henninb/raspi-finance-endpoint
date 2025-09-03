# Medical Expense Tracking Feature Implementation Plan

## Current Status (Updated 2025-08-28)
📍 **Phase 2 COMPLETED** ✅ - Medical expense entity fully implemented and operational
📋 **Next Recommended Phase**: **Phase 3** - Insurance Plan Management (2-3 days, medium risk)

### Quick Status Summary
- ✅ **Phase 1**: Account Type Extensions (28+ types) & Medical Provider Entity - **COMPLETED**
- ✅ **Phase 1.5**: Family Member Entity for 6-member family tracking - **COMPLETED**
- ✅ **Phase 2**: Core Medical Expense Entity (**100% COMPLETE**) - **ALL SYSTEMS OPERATIONAL**
- ⏳ **Phase 3**: Insurance Plan Management - **PENDING**
- ⏳ **Phase 4**: Deductible and Out-of-Pocket Tracking - **PENDING**
- ⏳ **Phase 5**: Medical Categories and Service Types - **PENDING**

## Overview
This document outlines a comprehensive plan to add medical expense tracking capabilities to the raspi-finance-endpoint application. The implementation will leverage the existing transaction system while extending it with medical-specific functionality.

## Executive Summary - PHASE 2 COMPLETE ✅
- **Approach**: ✅ **SUCCESSFUL** - Extended existing transaction system without breaking existing functionality
- **Strategy**: ✅ **COMPLETE** - All incremental, non-breaking Flyway migrations successfully deployed
- **Architecture**: ✅ **OPERATIONAL** - Full JPA entity integration with proper relationships
- **Data Integrity**: ✅ **VALIDATED** - All existing constraints maintained, new FK relationships working
- **Production Status**: ✅ **READY** - Complete medical expense system operational and tested

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

### Phase 1: Account Type Extensions and Provider Management ✅ **COMPLETED**
**Duration**: 2 days (completed 2025-08-25)
**Risk**: Low
**Dependencies**: None
**Status**: ✅ **IMPLEMENTED AND TESTED**

#### 1.1 ✅ Extend AccountType Enum - **COMPLETED**
**Implementation**: Comprehensive account type extension beyond just medical types
- **Files**: `V07__extend-account-types-comprehensive.sql` (all environments)
- **Enhanced Scope**: Extended to 28+ comprehensive account types including:
  - **Medical**: HSA, FSA, medical_savings
  - **Banking**: checking, savings, credit_card, certificate, money_market
  - **Investment**: brokerage, retirement_401k, retirement_ira, retirement_roth, pension
  - **Loan/Debt**: mortgage, auto_loan, student_loan, personal_loan, line_of_credit
  - **Utility**: utility, prepaid, gift_card
  - **Business**: business_checking, business_savings, business_credit
  - **Other**: cash, escrow, trust
- **Domain Updates**: `AccountType.kt` with categorization system (asset/liability/expense)
- **Converter Updates**: `AccountTypeConverter.kt` handles all new types
- **Testing**: All unit and functional tests updated and passing

#### 1.2 ✅ Create Medical Provider Entity - **COMPLETED**
**Implementation**: Comprehensive medical provider management system
- **Files**: `V08__create-medical-provider.sql` (all environments)
- **Enhanced Schema**: Full provider information including:
  - Provider identification (NPI, tax ID, billing name)
  - Complete address and contact information
  - Network status tracking (in_network, out_of_network, unknown)
  - Provider categorization with 12+ provider types
  - Comprehensive validation constraints
- **Domain Entity**: `MedicalProvider.kt` with full Jakarta validation
- **Converter**: `MedicalProviderTypeConverter.kt` for enum handling
- **Indexes**: Performance-optimized indexes for common queries
- **Testing**: Integration tests in all environments (prod, stage, func, int, oracle)

#### 1.3 ✅ Domain Entity Updates - **COMPLETED**
**Implemented Components**:
```kotlin
// AccountType.kt - Comprehensive 28+ account types with categorization
enum class AccountType(val label: String, val category: String) {
    // Medical accounts
    HSA("hsa", "asset"),
    FSA("fsa", "asset"),
    MedicalSavings("medical_savings", "asset"),

    // Investment accounts
    Brokerage("brokerage", "asset"),
    Retirement401k("retirement_401k", "asset"),
    RetirementIRA("retirement_ira", "asset"),

    // And 22 more comprehensive types...

    companion object {
        fun getMedicalTypes(): List<AccountType> = listOf(HSA, FSA, MedicalSavings)
        fun getInvestmentTypes(): List<AccountType> = ...
    }
}

// MedicalProvider.kt - Complete entity with validation
@Entity
@Table(name = "t_medical_provider")
data class MedicalProvider(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_id")
    var providerId: Long = 0L,

    @Column(name = "provider_name", nullable = false)
    @Convert(converter = LowerCaseConverter::class)
    var providerName: String,

    @Column(name = "provider_type", nullable = false)
    @Convert(converter = MedicalProviderTypeConverter::class)
    var providerType: MedicalProviderType = MedicalProviderType.General,

    // Complete address, contact, and network status fields...
)
```

**Database Migrations Status**:
- ✅ **Production** (`V07__extend-account-types-comprehensive.sql`, `V08__create-medical-provider.sql`)
- ✅ **Production Oracle** (`V05__extend-account-types-comprehensive.sql`, `V06__create-medical-provider.sql`)
- ✅ **Stage** (`V02__extend-account-types-comprehensive.sql`, `V03__create-medical-provider.sql`)
- ✅ **Functional Test** (`V03__extend-account-types-comprehensive.sql`, `V04__create-medical-provider.sql`)
- ✅ **Integration Test** (`V03__extend-account-types-comprehensive.sql`, `V04__create-medical-provider.sql`)
- ✅ **Oracle Test** (`V03__extend-account-types-comprehensive.sql`, `V04__create-medical-provider.sql`)

**Testing Status**:
- ✅ **Unit Tests**: All passing with updated account types
- ✅ **Integration Tests**: Database migrations working across all environments
- ✅ **Functional Tests**: End-to-end API tests passing with new account types
- ✅ **Build Verification**: Clean build success with all environments

**Phase 1 Deliverables**: ✅ **ALL COMPLETED**
- [x] Extended AccountType enum with 28+ comprehensive financial account types
- [x] Medical Provider entity with complete provider management capabilities
- [x] Database migrations for all environments (6 migration files)
- [x] Domain entities with proper validation and converters
- [x] Comprehensive testing across all environments
- [x] Build and deployment verification

### Phase 1.5: Family Member Entity ✅ **COMPLETED**
**Duration**: 1 day (completed 2025-08-25)
**Risk**: Low
**Dependencies**: Phase 1
**Status**: ✅ **IMPLEMENTED AND TESTED**

#### 1.5.1 ✅ Family Member Entity - **COMPLETED**
**Implementation**: Comprehensive family member tracking system for 6+ family members
- **Files**: `FamilyMember.kt`, `FamilyRelationship.kt`, `FamilyRelationshipConverter.kt`
- **Database Migrations**: `V09__create-family-member.sql` (all environments)
- **Family Management Features**:
  - Individual family member tracking (self, spouse, child, dependent, other)
  - Insurance member ID tracking for each family member
  - Medical record number and SSN (last 4 digits) support
  - Date of birth tracking
  - Full audit trail with active status management

#### 1.5.2 ✅ Family Relationship Enum - **COMPLETED**
**Implementation**: Comprehensive relationship tracking
- **Domain Entity**: `FamilyRelationship.kt` with 5 relationship types
- **Converter**: `FamilyRelationshipConverter.kt` for database mapping
- **Supported Relationships**: Self, Spouse, Child, Dependent, Other
- **JSON Support**: Full Jackson serialization/deserialization

#### 1.5.3 ✅ Database Schema Updates - **COMPLETED**
**Implemented Components**:
```sql
-- Family Member table with comprehensive tracking
CREATE TABLE t_family_member (
    family_member_id    BIGSERIAL PRIMARY KEY,
    owner               TEXT NOT NULL,           -- Links to account owner
    member_name         TEXT NOT NULL,           -- Family member name
    relationship        TEXT NOT NULL,           -- self, spouse, child, etc.
    date_of_birth       DATE,                   -- Birth date
    insurance_member_id TEXT,                   -- Insurance card member ID
    ssn_last_four      TEXT,                    -- Last 4 SSN digits
    medical_record_number TEXT,                 -- Hospital MRN
    active_status      BOOLEAN DEFAULT TRUE,    -- Active status
    -- Full audit fields and constraints
    CONSTRAINT uk_family_member_owner_name UNIQUE (owner, member_name)
);
```

**Database Migrations Status**:
- ✅ **Production** (`V09__create-family-member.sql`)
- ✅ **Production Oracle** (`V07__create-family-member.sql`)
- ✅ **Stage** (`V04__create-family-member.sql`)
- ✅ **Functional Test** (`V05__create-family-member.sql`)
- ✅ **Integration Test** (`V05__create-family-member.sql`)
- ✅ **Oracle Test** (`V05__create-family-member.sql`)

**Testing Status**:
- ✅ **Unit Tests**: FamilyMemberSpec and FamilyRelationshipSpec with comprehensive validation
- ✅ **Migration Tests**: All environments successfully apply family member table creation
- ✅ **Integration Tests**: Functional test environment validates family member table structure
- ✅ **Build Verification**: Clean build success with family member entities

**Phase 1.5 Benefits Achieved**:
- [x] **6-Member Family Support**: Track all family members with relationships
- [x] **Insurance Integration**: Each member can have unique insurance member ID
- [x] **Medical Record Tracking**: Link to hospital/clinic medical record numbers
- [x] **Privacy Protection**: Only stores last 4 SSN digits for identification
- [x] **Comprehensive Relationships**: Self, spouse, children, dependents, other
- [x] **Unique Constraints**: Prevent duplicate family member names per owner
- [x] **Full Audit Trail**: Date added/updated tracking for all family changes
- [x] **Test Data Seeding**: Pre-populated test family members in all test environments

**Family Member Reporting Capabilities Ready**:
```sql
-- Track medical expenses by family member (when Phase 2 is complete)
SELECT fm.member_name, fm.relationship, COUNT(*) as claim_count
FROM t_family_member fm
WHERE fm.owner = 'your_owner' AND fm.active_status = true
GROUP BY fm.member_name, fm.relationship;

-- Family hierarchy view
SELECT owner,
       SUM(CASE WHEN relationship = 'self' THEN 1 ELSE 0 END) as primary_members,
       SUM(CASE WHEN relationship = 'child' THEN 1 ELSE 0 END) as children,
       SUM(CASE WHEN relationship = 'dependent' THEN 1 ELSE 0 END) as dependents
FROM t_family_member
WHERE active_status = true
GROUP BY owner;
```

### Phase 2: Core Medical Expense Entity ✅ **100% COMPLETED**
**Duration**: 3 days (completed 2025-08-28)
**Risk**: Low (all major issues resolved)
**Dependencies**: Phase 1 and Phase 1.5
**Status**: ✅ **100% COMPLETE - ALL SYSTEMS OPERATIONAL**

#### 2.1 ✅ Medical Expense Entity Implementation - **COMPLETED**
**Major Achievement**: Full TDD implementation with comprehensive validation framework
- **Files**: `MedicalExpense.kt`, `ClaimStatus.kt`, `ClaimStatusConverter.kt`
- **Database Migration**: `V10__create-medical-expense.sql` (all environments)
- **Entity Features**:
  - Complete medical expense tracking with financial breakdown
  - Service date vs transaction date distinction
  - Insurance claim status management (6 states: submitted, processing, approved, denied, paid, closed)
  - Provider and family member relationship support
  - Financial consistency validation (allocated amounts cannot exceed billed amount)
  - Full audit trail with active status management

#### 2.2 ✅ Advanced Business Logic Validation - **COMPLETED USING TDD**
**Critical Success**: Applied Test-Driven Development to solve complex validation integration issues
- **TDD Methodology Applied**: Red-Green-Refactor cycle used to systematically fix all validation issues
- **Jakarta Validation Integration**: Fixed Kotlin interoperability issues with @field: targeting
- **Financial Consistency Rules**: Implemented @PrePersist/@PreUpdate validation ensuring:
  ```kotlin
  // Ensures: insuranceDiscount + insurancePaid + patientResponsibility <= billedAmount
  private fun validateFinancialConsistency() {
      val totalAllocated = insuranceDiscount + insurancePaid + patientResponsibility
      if (totalAllocated > billedAmount) {
          throw IllegalStateException("Total allocated amount ($totalAllocated) cannot exceed billed amount ($billedAmount)")
      }
  }
  ```
- **Domain Model Validation**: Complete integration with Spring CGLIB proxy system
- **Date Validation**: Fixed DateValidator null handling for optional date fields

#### 2.3 ✅ Repository and Service Layer - **COMPLETED**
**Implementation**: Full repository and service architecture
- **Repository**: `MedicalExpenseRepository.kt` with 15+ query methods
- **Service**: `MedicalExpenseService.kt` and `MedicalExpenseServiceImpl.kt` with business logic
- **Query Capabilities**:
  - Find by transaction ID, provider ID, family member ID
  - Date range queries with service date filtering
  - Claim status and payment status filtering
  - Financial totals and calculations by year
  - Outstanding balance and unpaid claim tracking

#### 2.4 ✅ Integration Test Suite - **100% PASSING**
**Testing Success**: Comprehensive test coverage with complete TDD implementation
- **Test Files**:
  - `MedicalExpenseRepositoryIntSpec.groovy` (All tests passing)
  - `MedicalExpenseControllerExtendedIsolatedSpec.groovy` (All functional tests passing)
- **Test Coverage**:
  - ✅ **CRUD Operations**: Complete create, read, update, delete functionality
  - ✅ **Financial Calculations**: All billed amounts, insurance payments, patient responsibility calculations
  - ✅ **Query Methods**: Date range filtering, claim status queries, provider/family member lookups
  - ✅ **Validation Framework**: Complete financial consistency, required fields, constraint validation
  - ✅ **FK Relationships**: Transaction, Provider, and FamilyMember associations working correctly
  - ✅ **REST Endpoints**: All HTTP endpoints responding with correct status codes (201, 200, 404, etc.)
- **Production Validation**:
  - Complete entity validation with Jakarta validation annotations
  - Financial business rules enforced at entity level
  - All database constraints functioning properly
  - Controller layer handling all success and error scenarios

#### 2.5 ✅ **IMPLEMENTATION COMPLETE** - All Integration Tests Passing
**Current Status**: All integration and functional tests passing (100% success rate)
**Test Results**:
1. ✅ **Integration Tests**: `MedicalExpenseRepositoryIntSpec` - All repository methods working
2. ✅ **Functional Tests**: `MedicalExpenseControllerExtendedIsolatedSpec` - All endpoints operational
3. ✅ **Entity Validation**: Complete TDD validation framework operational
4. ✅ **Service Layer**: All business logic methods functioning correctly
5. ✅ **Controller Layer**: REST endpoints responding correctly with proper HTTP status codes

**Implementation Achievements**:
- ✅ **Financial validation**: Complete validation framework for all financial calculations
- ✅ **Foreign key relationships**: All FK constraints working correctly with Transaction, Provider, and FamilyMember entities
- ✅ **Repository methods**: All CRUD and query methods operational
- ✅ **Business logic validation**: Complete @PrePersist/@PreUpdate validation for financial consistency

**Production Ready**: Medical expense system fully operational and ready for production use

#### 2.6 ✅ Database Schema - **FULLY IMPLEMENTED**
**Comprehensive Schema**: Advanced medical expense tracking with all necessary constraints
```sql
CREATE TABLE IF NOT EXISTS public.t_medical_expense (
    medical_expense_id          BIGSERIAL PRIMARY KEY,
    transaction_id              BIGINT NOT NULL,
    provider_id                 BIGINT,                    -- Optional provider reference
    family_member_id           BIGINT,                    -- Optional family member reference

    -- Core medical expense data
    service_date                DATE NOT NULL,             -- Date service was provided
    service_description         TEXT,                      -- Description of medical service
    procedure_code              TEXT,                      -- CPT/HCPCS procedure codes
    diagnosis_code              TEXT,                      -- ICD-10 diagnosis codes

    -- Financial breakdown with validation
    billed_amount              NUMERIC(12,2) DEFAULT 0.00 NOT NULL,    -- Original amount billed
    insurance_discount         NUMERIC(12,2) DEFAULT 0.00 NOT NULL,    -- Insurance discount/adjustment
    insurance_paid             NUMERIC(12,2) DEFAULT 0.00 NOT NULL,    -- Amount insurance paid
    patient_responsibility     NUMERIC(12,2) DEFAULT 0.00 NOT NULL,    -- Patient's portion
    paid_date                  DATE,                                   -- Date patient paid

    -- Insurance and claim tracking
    is_out_of_network          BOOLEAN DEFAULT FALSE NOT NULL,         -- Network status
    claim_number               TEXT,                                   -- Insurance claim number
    claim_status               TEXT DEFAULT 'submitted' NOT NULL,      -- Claim processing status

    -- Audit and management
    active_status              BOOLEAN DEFAULT TRUE NOT NULL,
    date_added                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    date_updated               TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Foreign key constraints with CASCADE DELETE
    CONSTRAINT fk_medical_transaction FOREIGN KEY (transaction_id)
        REFERENCES public.t_transaction(transaction_id) ON DELETE CASCADE,
    CONSTRAINT fk_medical_provider FOREIGN KEY (provider_id)
        REFERENCES public.t_medical_provider(provider_id),
    CONSTRAINT fk_medical_family_member FOREIGN KEY (family_member_id)
        REFERENCES public.t_family_member(family_member_id),

    -- Business rule constraints
    CONSTRAINT ck_claim_status CHECK (claim_status IN (
        'submitted', 'processing', 'approved', 'denied', 'paid', 'closed'
    )),
    CONSTRAINT ck_financial_amounts CHECK (
        billed_amount >= 0 AND insurance_discount >= 0 AND
        insurance_paid >= 0 AND patient_responsibility >= 0
    ),
    CONSTRAINT ck_service_date_valid CHECK (service_date <= CURRENT_DATE)
);

-- Performance optimization indexes
CREATE UNIQUE INDEX idx_medical_expense_transaction ON public.t_medical_expense(transaction_id);
CREATE INDEX idx_medical_expense_provider ON public.t_medical_expense(provider_id);
CREATE INDEX idx_medical_expense_family_member ON public.t_medical_expense(family_member_id);
CREATE INDEX idx_medical_expense_service_date ON public.t_medical_expense(service_date);
CREATE INDEX idx_medical_expense_claim_status ON public.t_medical_expense(claim_status);
CREATE INDEX idx_medical_expense_active ON public.t_medical_expense(active_status) WHERE active_status = true;
```

**Migration Status Across All Environments**:
- ✅ **Production**: `V10__create-medical-expense.sql`
- ✅ **Production Oracle**: `V08__create-medical-expense.sql`
- ✅ **Stage**: `V05__create-medical-expense.sql`
- ✅ **Functional Test**: `V06__create-medical-expense.sql`
- ✅ **Integration Test**: `V06__create-medical-expense.sql`
- ✅ **Oracle Test**: `V06__create-medical-expense.sql`

#### 2.7 ✅ Domain Entity Implementation - **COMPLETED**
**Full-Featured Entity**: Comprehensive medical expense domain model with advanced validation
```kotlin
@Entity
@Table(name = "t_medical_expense")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class MedicalExpense(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medical_expense_id")
    var medicalExpenseId: Long = 0L,

    @field:Min(value = 0L)
    @Column(name = "transaction_id", nullable = false)
    var transactionId: Long = 0L,

    @Column(name = "provider_id")
    var providerId: Long? = null,

    @Column(name = "family_member_id")
    var familyMemberId: Long? = null,

    @field:ValidDate
    @Column(name = "service_date", nullable = false)
    var serviceDate: Date,

    @field:Size(max = 200, message = FILED_MUST_BE_BETWEEN_ZERO_AND_TWO_HUNDRED_MESSAGE)
    @field:Pattern(regexp = ASCII_PATTERN, message = FIELD_MUST_BE_ASCII_MESSAGE)
    @Column(name = "service_description")
    @field:Convert(converter = LowerCaseConverter::class)
    var serviceDescription: String? = "",

    // Financial fields with validation
    @field:Digits(integer = 12, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "billed_amount", nullable = false)
    var billedAmount: BigDecimal = BigDecimal("0.00"),

    // ... other financial fields with similar validation

    @field:Convert(converter = ClaimStatusConverter::class)
    @Column(name = "claim_status", nullable = false)
    var claimStatus: ClaimStatus = ClaimStatus.Submitted,

    // Audit fields
    @Column(name = "active_status", nullable = false)
    var activeStatus: Boolean = true

) {
    // Financial consistency validation
    @PrePersist
    @PreUpdate
    fun validateFinancialConsistency() {
        val totalAllocated = insuranceDiscount + insurancePaid + patientResponsibility
        if (totalAllocated > billedAmount) {
            throw IllegalStateException(
                "Total allocated amount ($totalAllocated) cannot exceed billed amount ($billedAmount)"
            )
        }
    }

    // Calculated fields
    fun getNetPatientAmount(): BigDecimal =
        billedAmount - insuranceDiscount - insurancePaid

    fun isFullyPaid(): Boolean =
        patientResponsibility == BigDecimal.ZERO || paidDate != null

    fun isOutstanding(): Boolean =
        patientResponsibility > BigDecimal.ZERO && paidDate == null
}
```

#### 2.8 ✅ TDD SUCCESS STORY - **COMPREHENSIVE VALIDATION FRAMEWORK IMPLEMENTED**
**Applied Test-Driven Development**: Systematic approach to solve complex integration issues

**Phase 1 - RED**: Isolated exact validation failures through focused test methods
- Created `TDD Step 1: Test Transaction entity creation independently`
- Created `TDD Step 2: Test MedicalExpense entity creation`
- Identified root causes: Jakarta validation + Kotlin interoperability, DateValidator nulls, Spring CGLIB proxy issues

**Phase 2 - GREEN**: Fixed each validation constraint with specific error message targeting
- **Jakarta Validation Fix**: Changed from `@Size` to `@field:Size` for Kotlin property targeting
- **DateValidator Fix**: Updated signature from `isValid(value: Date, ...)` to `isValid(value: Date?, ...)`
- **Spring CGLIB Fix**: Added `open` modifier to Kotlin service classes
- **Financial Validation**: Implemented @PrePersist/@PreUpdate business rule validation

**Phase 3 - REFACTOR**: Applied validated patterns to fix helper methods and test data construction
- Fixed `createTestMedicalExpense()` helper method with financially consistent test data
- Updated all test methods to use generated entity IDs instead of hardcoded parameter IDs
- Resolved foreign key constraint violations by using null references appropriately

**TDD Results**:
- ✅ **Initial 8 unit tests**: FIXED with Jakarta validation framework
- ✅ **All integration tests**: 100% PASSING (complete success rate achieved)
- ✅ **All functional tests**: 100% PASSING with isolated test architecture
- ✅ **Validation framework**: Fully operational across all entity types
- ✅ **Financial business rules**: Enforced at entity level with clear error messages
- ✅ **Test data consistency**: All helper methods create valid, consistent test entities
- ✅ **Production validation**: Complete end-to-end testing from API to database

### Phase 2: Core Medical Expense Entity ✅ **COMPLETED SUCCESSFULLY**
**Duration**: 3 days (August 26-28, 2025)
**Risk**: Low (all major challenges resolved)
**Dependencies**: Phase 1 and Phase 1.5 (Family Member Entity) - **ALL MET**

#### 2.1 Create Medical Expense Entity (V10__create-medical-expense.sql) - **UPDATED FOR FAMILY SUPPORT**
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

## Implementation Status Summary (August 28, 2025) 🎉

### Major Achievement: Phase 2 Complete - Medical Expense System Operational

**🏆 PHASE 2 COMPLETED SUCCESSFULLY**: The core medical expense tracking system is now **100% operational** and ready for production use.

### Technical Accomplishments

**✅ Complete System Architecture**:
- **Entity Layer**: `MedicalExpense.kt` with comprehensive validation and business rules
- **Repository Layer**: `MedicalExpenseRepository.kt` with 15+ query methods
- **Service Layer**: `MedicalExpenseService.kt` with complete business logic implementation
- **Controller Layer**: `MedicalExpenseController.kt` with REST endpoints
- **Database Schema**: Complete `t_medical_expense` table with optimized indexes

**✅ Advanced Features Implemented**:
- **Financial Consistency Validation**: Automatic validation ensuring allocated amounts don't exceed billed amounts
- **Claim Status Management**: 6-state claim workflow (submitted, processing, approved, denied, paid, closed)
- **Provider Integration**: Optional linkage to medical providers with network status tracking
- **Family Member Support**: Track medical expenses by family member relationships
- **Service Date vs Transaction Date**: Distinguish between service date and payment date
- **Insurance Tracking**: Out-of-network status, claim numbers, and insurance payment tracking

**✅ Production-Ready Validation**:
- **Integration Tests**: All repository methods validated with H2 database
- **Functional Tests**: Complete REST API testing with isolated test architecture
- **TDD Implementation**: Systematic test-driven development resolving all validation challenges
- **Entity Validation**: Complete Jakarta validation framework with financial business rules
- **Database Constraints**: All FK relationships and check constraints operational

### Business Value Delivered

**🩺 Medical Expense Tracking Capabilities**:
- Track complete financial breakdown: billed amount, insurance discount, insurance payment, patient responsibility
- Manage medical provider information with network status
- Link expenses to family members for comprehensive family medical tracking
- Monitor claim status progression from submission to payment
- Calculate financial metrics: coverage percentage, net amounts, outstanding balances

**📊 Reporting and Analytics Ready**:
- Query expenses by date range, provider, family member, claim status
- Financial totals and calculations by year
- Outstanding balance tracking for unpaid patient responsibilities
- Insurance coverage analysis and out-of-network expense monitoring

### Integration Success

**🔗 Seamless System Integration**:
- **Transaction System**: Medical expenses link directly to existing transaction records
- **Account Management**: Leverages existing account types (HSA, FSA, medical_savings)
- **Category System**: Integrates with existing transaction categorization
- **Receipt Management**: Compatible with existing receipt image storage
- **Audit Trail**: Maintains consistent date_added/date_updated patterns

**📈 Non-Breaking Implementation**:
- All existing functionality preserved during medical expense addition
- Incremental Flyway migrations successfully deployed across all environments
- Backward compatibility maintained for existing transaction processing
- No performance impact on existing operations

### Next Phase Readiness

**⏳ Phase 3 Preparation - Insurance Plan Management**:
- Foundation established for insurance plan entity creation
- Database relationships prepared for insurance plan linkage
- Service layer architecture ready for insurance business logic expansion
- Advanced deductible and out-of-pocket tracking capabilities planned

**🎯 Production Deployment Status**: **READY**
- All validation tests passing
- Database migrations tested across all environments
- REST endpoints operational and documented
- Error handling and edge cases covered
- Performance optimizations implemented with proper indexing

The medical expense tracking system represents a **major milestone** in the raspi-finance application's evolution, providing comprehensive healthcare financial management capabilities while maintaining the system's architectural integrity and performance standards.

### Phase 2.5: Medical Expense Payment Decoupling ⏳ **PENDING**
**Duration**: 2 days
**Risk**: Low
**Dependencies**: Phase 2
**Status**: 🔄 **DESIGN PHASE - READY FOR IMPLEMENTATION**

#### Overview
Redesign the medical expense system to decouple expense creation from payment transactions. This allows medical expenses to be created independently and linked to payment transactions later when payment is actually made.

#### Current Design Issues
- Medical expenses currently require a `transaction_id` at creation time
- This forces artificial coupling between expense occurrence and payment
- Real workflow: Medical service happens → Bill received → Payment made later

#### Proposed Design Changes

##### 2.5.1 Database Schema Updates (V11__decouple-medical-expense-payments.sql)
```sql
-- Make transaction_id nullable and add paid_amount field
ALTER TABLE public.t_medical_expense
ALTER COLUMN transaction_id DROP NOT NULL;

-- Add paid_amount field to track actual payments
ALTER TABLE public.t_medical_expense
ADD COLUMN paid_amount NUMERIC(12,2) DEFAULT 0.00 NOT NULL;

-- Add constraint to ensure paid_amount is non-negative
ALTER TABLE public.t_medical_expense
ADD CONSTRAINT ck_paid_amount_non_negative CHECK (paid_amount >= 0);

-- Update existing records to sync paid_amount with patient_responsibility where transaction exists
UPDATE public.t_medical_expense
SET paid_amount = patient_responsibility
WHERE transaction_id IS NOT NULL;
```

##### 2.5.2 Entity Updates (MedicalExpense.kt)
```kotlin
@Entity
@Table(name = "t_medical_expense")
data class MedicalExpense(
    // ... existing fields ...

    @Column(name = "transaction_id")  // Remove nullable = false
    var transactionId: Long? = null,  // Make nullable

    // Add new paid_amount field
    @field:Digits(integer = 12, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "paid_amount", nullable = false)
    var paidAmount: BigDecimal = BigDecimal("0.00"),

    // ... existing fields ...
) {
    // Add helper methods for payment status
    fun getUnpaidAmount(): BigDecimal =
        (patientResponsibility - paidAmount).max(BigDecimal.ZERO)

    fun isFullyPaid(): Boolean =
        paidAmount >= patientResponsibility

    fun isPartiallyPaid(): Boolean =
        paidAmount > BigDecimal.ZERO && paidAmount < patientResponsibility

    fun isUnpaid(): Boolean =
        paidAmount == BigDecimal.ZERO

    fun isOverpaid(): Boolean =
        paidAmount > patientResponsibility
}
```

##### 2.5.3 Service Layer Updates
```kotlin
interface MedicalExpenseService {
    // ... existing methods ...

    // New payment-related methods
    fun linkPaymentTransaction(medicalExpenseId: Long, transactionId: Long): MedicalExpense
    fun unlinkPaymentTransaction(medicalExpenseId: Long): MedicalExpense
    fun updatePaidAmount(medicalExpenseId: Long): MedicalExpense  // Sync with transaction amount
    fun findUnpaidMedicalExpenses(): List<MedicalExpense>
    fun findPartiallyPaidMedicalExpenses(): List<MedicalExpense>
}

@Service
class MedicalExpenseServiceImpl(
    private val medicalExpenseRepository: MedicalExpenseRepository,
    private val transactionRepository: TransactionRepository
) : MedicalExpenseService {

    @Transactional
    fun linkPaymentTransaction(medicalExpenseId: Long, transactionId: Long): MedicalExpense {
        val medicalExpense = medicalExpenseRepository.findById(medicalExpenseId)
            .orElseThrow { EntityNotFoundException("Medical expense not found: $medicalExpenseId") }

        val transaction = transactionRepository.findById(transactionId)
            .orElseThrow { EntityNotFoundException("Transaction not found: $transactionId") }

        medicalExpense.transactionId = transactionId
        medicalExpense.paidAmount = transaction.amount.abs()  // Use absolute value for payment

        return medicalExpenseRepository.save(medicalExpense)
    }

    @Transactional
    fun updatePaidAmount(medicalExpenseId: Long): MedicalExpense {
        val medicalExpense = medicalExpenseRepository.findById(medicalExpenseId)
            .orElseThrow { EntityNotFoundException("Medical expense not found: $medicalExpenseId") }

        if (medicalExpense.transactionId != null) {
            val transaction = transactionRepository.findById(medicalExpense.transactionId!!)
                .orElseThrow { EntityNotFoundException("Transaction not found: ${medicalExpense.transactionId}") }

            medicalExpense.paidAmount = transaction.amount.abs()
            return medicalExpenseRepository.save(medicalExpense)
        }

        return medicalExpense
    }
}
```

##### 2.5.4 Repository Updates
```kotlin
interface MedicalExpenseRepository : JpaRepository<MedicalExpense, Long> {
    // ... existing methods ...

    // New payment-related queries
    @Query("SELECT me FROM MedicalExpense me WHERE me.paidAmount = 0 AND me.activeStatus = true")
    fun findUnpaidMedicalExpenses(): List<MedicalExpense>

    @Query("SELECT me FROM MedicalExpense me WHERE me.paidAmount > 0 AND me.paidAmount < me.patientResponsibility AND me.activeStatus = true")
    fun findPartiallyPaidMedicalExpenses(): List<MedicalExpense>

    @Query("SELECT me FROM MedicalExpense me WHERE me.paidAmount >= me.patientResponsibility AND me.activeStatus = true")
    fun findFullyPaidMedicalExpenses(): List<MedicalExpense>

    @Query("SELECT me FROM MedicalExpense me WHERE me.transactionId IS NULL AND me.activeStatus = true")
    fun findMedicalExpensesWithoutTransaction(): List<MedicalExpense>
}
```

##### 2.5.5 Controller Updates
```kotlin
@RestController
@RequestMapping("/medical-expenses")
class MedicalExpenseController(
    private val medicalExpenseService: MedicalExpenseService
) {
    // ... existing endpoints ...

    @PostMapping("/{medicalExpenseId}/payments/{transactionId}")
    fun linkPaymentTransaction(
        @PathVariable medicalExpenseId: Long,
        @PathVariable transactionId: Long
    ): ResponseEntity<MedicalExpense> {
        val updated = medicalExpenseService.linkPaymentTransaction(medicalExpenseId, transactionId)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{medicalExpenseId}/payments")
    fun unlinkPaymentTransaction(
        @PathVariable medicalExpenseId: Long
    ): ResponseEntity<MedicalExpense> {
        val updated = medicalExpenseService.unlinkPaymentTransaction(medicalExpenseId)
        return ResponseEntity.ok(updated)
    }

    @GetMapping("/unpaid")
    fun getUnpaidMedicalExpenses(): List<MedicalExpense> {
        return medicalExpenseService.findUnpaidMedicalExpenses()
    }

    @GetMapping("/partially-paid")
    fun getPartiallyPaidMedicalExpenses(): List<MedicalExpense> {
        return medicalExpenseService.findPartiallyPaidMedicalExpenses()
    }

    @PutMapping("/{medicalExpenseId}/sync-payment")
    fun syncPaymentAmount(
        @PathVariable medicalExpenseId: Long
    ): ResponseEntity<MedicalExpense> {
        val updated = medicalExpenseService.updatePaidAmount(medicalExpenseId)
        return ResponseEntity.ok(updated)
    }
}
```

#### Implementation Benefits

##### Workflow Improvements
1. **Natural Workflow**: Create medical expense when service occurs, link payment later
2. **Flexible Payment Timing**: No forced coupling between expense and payment dates
3. **UI Simplicity**: Show unpaid expenses for payment selection
4. **Payment Independence**: Can create expenses without knowing payment method

##### Data Integrity
1. **Optional Payments**: Medical expenses can exist without payments
2. **Payment Tracking**: `paid_amount` accurately reflects actual payments
3. **Overpayment Handling**: System can track overpayments without validation errors
4. **Audit Trail**: Maintain complete history of expense creation vs payment

##### Reporting Capabilities
1. **Unpaid Expenses**: Easy identification of outstanding medical bills
2. **Payment Status**: Clear distinction between paid, unpaid, and partially paid
3. **Cash Flow**: Better understanding of pending medical payments
4. **Financial Planning**: Track upcoming medical payment obligations

#### Testing Strategy

##### Unit Tests Updates
```groovy
class MedicalExpenseSpec extends Specification {
    def "should create medical expense without transaction"() {
        given: "a medical expense without transaction"
        def expense = new MedicalExpense(
            serviceDate: Date.valueOf("2024-01-15"),
            billedAmount: new BigDecimal("250.00"),
            patientResponsibility: new BigDecimal("50.00"),
            paidAmount: BigDecimal.ZERO,
            transactionId: null
        )

        expect: "expense is valid and unpaid"
        expense.isUnpaid()
        expense.getUnpaidAmount() == new BigDecimal("50.00")
    }

    def "should link payment transaction and update paid amount"() {
        given: "an unpaid medical expense"
        def expense = createUnpaidMedicalExpense()

        when: "transaction is linked"
        expense.transactionId = 123L
        expense.paidAmount = new BigDecimal("50.00")

        then: "expense is fully paid"
        expense.isFullyPaid()
        expense.getUnpaidAmount() == BigDecimal.ZERO
    }
}
```

##### Integration Tests Updates
```groovy
class MedicalExpenseRepositoryIntSpec extends Specification {
    def "should find unpaid medical expenses"() {
        given: "mix of paid and unpaid expenses"
        createPaidMedicalExpense()
        def unpaidExpense = createUnpaidMedicalExpense()

        when: "searching for unpaid expenses"
        def unpaidExpenses = medicalExpenseRepository.findUnpaidMedicalExpenses()

        then: "only unpaid expenses are returned"
        unpaidExpenses.size() == 1
        unpaidExpenses[0].medicalExpenseId == unpaidExpense.medicalExpenseId
    }
}
```

##### Functional Tests Updates
```groovy
class MedicalExpenseControllerIsolatedSpec extends Specification {
    def "should create medical expense without transaction"() {
        given: "medical expense data without transaction"
        def expenseData = [
            serviceDate: "2024-01-15",
            billedAmount: "250.00",
            patientResponsibility: "50.00"
        ]

        when: "creating medical expense"
        def response = restTemplate.postForEntity("/medical-expenses", expenseData, Map)

        then: "expense is created successfully"
        response.statusCode == HttpStatus.CREATED
        response.body.transactionId == null
        response.body.paidAmount == "0.00"
    }

    def "should link payment transaction to medical expense"() {
        given: "an existing unpaid medical expense"
        def expense = createUnpaidMedicalExpense()
        def transaction = createTestTransaction()

        when: "linking payment transaction"
        def response = restTemplate.postForEntity(
            "/medical-expenses/${expense.medicalExpenseId}/payments/${transaction.transactionId}",
            null, Map
        )

        then: "expense is updated with payment"
        response.statusCode == HttpStatus.OK
        response.body.transactionId == transaction.transactionId
        response.body.paidAmount == transaction.amount.abs().toString()
    }
}
```

#### Migration Considerations

##### Backward Compatibility
1. **Existing Data**: All existing medical expenses with transactions remain functional
2. **API Compatibility**: Existing endpoints continue to work
3. **Validation**: New validation allows but doesn't require transaction_id

##### Migration Steps
1. **Database**: Make transaction_id nullable, add paid_amount field
2. **Entity**: Update Kotlin entity with nullable transactionId and paidAmount
3. **Service**: Add payment linking methods
4. **Controller**: Add new payment management endpoints
5. **Tests**: Update test suites for new workflow

#### Risk Assessment

##### Low Risk Factors
1. **Additive Changes**: No breaking changes to existing functionality
2. **Gradual Migration**: Can implement incrementally
3. **Fallback**: Existing workflow still supported
4. **Simple Logic**: Payment linking is straightforward

##### Mitigation Strategies
1. **Thorough Testing**: Complete test coverage for new payment workflows
2. **Database Migration**: Careful migration with proper rollback scripts
3. **Validation**: Ensure data consistency during transition
4. **Documentation**: Clear documentation of new workflow patterns

#### Phase 2.5 Deliverables
- [ ] Database migration V11__decouple-medical-expense-payments.sql
- [ ] Updated MedicalExpense.kt entity with nullable transactionId and paidAmount
- [ ] Enhanced MedicalExpenseService with payment linking methods
- [ ] New repository query methods for payment status filtering
- [ ] Updated REST endpoints for payment management
- [ ] Comprehensive test suite updates (unit, integration, functional)
- [ ] Migration scripts for existing data
- [ ] Documentation updates for new workflow

This design provides the flexibility you need for real-world medical expense management while maintaining system integrity and supporting both the new decoupled workflow and existing functionality during the transition period.