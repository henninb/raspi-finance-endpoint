-- Migration: V09__decouple-medical-expense-payments.sql (Oracle)
-- Purpose: Decouple medical expenses from transaction creation
-- Changes:
--   1. Make transaction_id nullable in t_medical_expense
--   2. Add paid_amount field to track actual payment amounts
--   3. Update existing records to sync paid_amount with patient_responsibility

-- Make transaction_id nullable to allow medical expenses without payments
ALTER TABLE t_medical_expense 
MODIFY transaction_id NUMBER(19) NULL;

-- Add paid_amount field to track actual payment amounts
ALTER TABLE t_medical_expense 
ADD paid_amount NUMBER(12,2) DEFAULT 0.00 NOT NULL;

-- Add constraint to ensure paid_amount is non-negative
ALTER TABLE t_medical_expense 
ADD CONSTRAINT ck_paid_amount_non_negative CHECK (paid_amount >= 0);

-- Update existing records to sync paid_amount with patient_responsibility where transaction exists
-- This maintains backward compatibility for existing medical expenses with transactions
UPDATE t_medical_expense 
SET paid_amount = patient_responsibility 
WHERE transaction_id IS NOT NULL;

-- Add comments to document the new field
COMMENT ON COLUMN t_medical_expense.paid_amount IS 'Actual amount paid by patient, synced with linked transaction amount';
COMMENT ON COLUMN t_medical_expense.transaction_id IS 'Optional reference to payment transaction, can be null for unpaid expenses';