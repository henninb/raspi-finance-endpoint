# Medical Claims Insert Process

This document outlines the process for inserting medical expense claims into the raspi-finance-endpoint database.

## Prerequisites

### Database Access
```bash
ssh debian-dockerserver 'docker exec postgresql-server psql -U henninb -d finance_db -c "<SQL_COMMAND>"'
```

### Required Tables and Relationships
- `t_medical_expense` - Main medical expense records
- `t_transaction` - Financial transactions (required for each medical expense)
- `t_family_member` - Patient/family member mapping
- `t_account` - Account for medical expenses
- `t_description` - Provider/description lookup
- `t_category` - Expense categorization

## Step 1: Verify Account and Family Members

### Check Medical Account
```sql
SELECT account_id, account_name_owner, account_type, active_status
FROM t_account
WHERE account_name_owner = 'medical_john' AND active_status = true;
```
Expected result: `account_id = 1045, account_type = credit`

### Check Family Members
```sql
SELECT family_member_id, member_name, relationship, owner
FROM t_family_member
WHERE active_status = true
ORDER BY member_name;
```
Expected mapping:
- `john smith` (self) = family_member_id: 81
- `jane smith` (spouse) = family_member_id: 82
- `alice smith` (child) = family_member_id: 84

## Step 2: Check Categories
```sql
SELECT category_name
FROM t_category
WHERE active_status = true
AND (category_name ILIKE '%medical%' OR category_name ILIKE '%health%')
ORDER BY category_name;
```
Use category: `medical`

## Step 3: Create Provider Descriptions

Medical expenses require descriptions to exist in `t_description` table:

```sql
INSERT INTO t_description (description_name, owner, active_status, date_added, date_updated)
VALUES
('provider name 1', 'john', true, NOW(), NOW()),
('provider name 2', 'john', true, NOW(), NOW())
ON CONFLICT (description_name) DO NOTHING;
```

## Step 4: Create Transactions

Each medical expense needs a corresponding transaction:

```sql
INSERT INTO t_transaction (
    guid, account_id, account_type, transaction_type, account_name_owner,
    transaction_date, description, category, amount, transaction_state,
    active_status, date_added, date_updated
) VALUES
(gen_random_uuid()::text, 1045, 'credit', 'expense', 'medical_john',
 '2025-08-08', 'provider name 1', 'medical', 150.70, 'outstanding',
 true, NOW(), NOW())
RETURNING transaction_id, description, amount;
```

### Field Mapping from CSV Data:
- `transaction_date` = Service Date
- `description` = Provider (lowercase)
- `amount` = "I Owe" amount (Patient Responsibility)
- `transaction_state` = 'outstanding' (for unpaid claims)
- `transaction_type` = 'expense'
- `account_type` = 'credit'

## Step 5: Create Medical Expense Records

Link medical expenses to transactions and family members:

```sql
INSERT INTO t_medical_expense (
    transaction_id, family_member_id, service_date, service_description,
    billed_amount, insurance_discount, insurance_paid, patient_responsibility,
    claim_status, claim_number, is_out_of_network, active_status,
    date_added, date_updated
) VALUES
(36127, 82, '2025-08-08', 'Provider Name 1', 267.04, 0.00, 0.00, 150.70,
 'approved', 'CLAIM-20250808-001', false, true, NOW(), NOW())
RETURNING medical_expense_id, transaction_id, service_description,
         billed_amount, patient_responsibility;
```

### Field Mapping from CSV Data:
- `service_date` = Service Date
- `service_description` = Provider (proper case)
- `billed_amount` = Billed amount
- `insurance_paid` = Plan Paid amount
- `patient_responsibility` = Patient Responsibility amount
- `claim_status` = 'approved' (for processed claims)
- `claim_number` = Generated unique identifier
- `family_member_id` = Map patient name to family_member_id

### Insurance Calculation:
- For fully covered claims: `insurance_paid` > 0, `patient_responsibility` = 0
- For unpaid claims: `insurance_paid` = 0, `patient_responsibility` = "I Owe" amount
- `insurance_discount` = typically 0 (unless specified)

## Step 6: Verification Query

Verify the inserted data:

```sql
SELECT
    me.service_date,
    fm.member_name as patient,
    me.service_description as provider,
    me.claim_status,
    me.billed_amount,
    me.insurance_paid as plan_paid,
    me.patient_responsibility,
    t.amount as transaction_amount,
    t.transaction_state
FROM t_medical_expense me
JOIN t_transaction t ON me.transaction_id = t.transaction_id
JOIN t_family_member fm ON me.family_member_id = fm.family_member_id
WHERE t.account_name_owner = 'medical_john'
  AND me.active_status = true
ORDER BY me.service_date DESC;
```

## Common CSV Data Format

```csv
Service Date,Patient,Provider,Status,Billed,Plan Paid,Patient Responsibility,I Owe,My Payments
8/8/2025,Jane,Provider Name,Processed,$267.04,$0.00,$150.70,$150.70,$0.00
```

## Key Constraints and Validations

### Database Constraints:
- `description` must exist in `t_description` table
- `category` must exist in `t_category` table
- `transaction_state` must be: cleared, outstanding, future, undefined
- `transaction_type` must be: expense, income, transfer, undefined
- `claim_status` must be: submitted, processing, approved, denied, paid, closed

### Financial Validation:
- `insurance_discount + insurance_paid + patient_responsibility <= billed_amount`
- Transaction amount should match patient responsibility for unpaid claims
- Transaction amount should be 0 for fully covered claims

## Error Handling

Common errors and solutions:
1. **Missing description**: Create entry in `t_description` table first
2. **Invalid transaction_state**: Use 'outstanding' for unpaid claims
3. **Invalid transaction_type**: Use 'expense' for medical costs
4. **Financial inconsistency**: Ensure amounts add up correctly
5. **Missing family member**: Verify family_member_id exists and is active

## Notes

- All text fields are stored in lowercase due to database constraints
- Claim numbers should be unique - use format: `CLAIM-YYYYMMDD-###`
- Use `gen_random_uuid()::text` for transaction GUIDs
- Set `is_out_of_network = false` unless specified otherwise
- Set `active_status = true` for all new records