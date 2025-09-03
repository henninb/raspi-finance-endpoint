-- Migration: V07__decouple-medical-expense-payments.sql
-- Purpose: Decouple medical expenses from transaction creation
-- Changes:
--   1. Make transaction_id nullable in t_medical_expense
--   2. Add paid_amount field to track actual payment amounts
--   3. Update existing records to sync paid_amount with patient_responsibility

-- Make transaction_id nullable to allow medical expenses without payments
ALTER TABLE public.t_medical_expense
ALTER COLUMN transaction_id DROP NOT NULL;

-- Add paid_amount field to track actual payment amounts
ALTER TABLE public.t_medical_expense
ADD COLUMN paid_amount NUMERIC(12,2) DEFAULT 0.00 NOT NULL;

-- Add constraint to ensure paid_amount is non-negative
ALTER TABLE public.t_medical_expense
ADD CONSTRAINT ck_paid_amount_non_negative CHECK (paid_amount >= 0);

-- Update existing records to sync paid_amount with patient_responsibility where transaction exists
-- This maintains backward compatibility for existing medical expenses with transactions
UPDATE public.t_medical_expense
SET paid_amount = patient_responsibility
WHERE transaction_id IS NOT NULL;