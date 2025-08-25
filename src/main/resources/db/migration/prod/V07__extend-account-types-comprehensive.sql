-- V07: Extend AccountType enum with comprehensive account types
-- Add support for medical, financial, investment, and utility account types

-- Drop existing constraint
ALTER TABLE public.t_account
DROP CONSTRAINT IF EXISTS ck_account_type;

-- Add new comprehensive account type constraint
ALTER TABLE public.t_account
ADD CONSTRAINT ck_account_type
CHECK (account_type IN (
    -- Existing types (preserve compatibility)
    'credit', 'debit', 'undefined',

    -- Banking/Traditional Accounts
    'checking', 'savings', 'credit_card', 'certificate', 'money_market',

    -- Investment Accounts
    'brokerage', 'retirement_401k', 'retirement_ira', 'retirement_roth', 'pension',

    -- Medical/Healthcare Accounts
    'hsa', 'fsa', 'medical_savings',

    -- Loan/Debt Accounts
    'mortgage', 'auto_loan', 'student_loan', 'personal_loan', 'line_of_credit',

    -- Utility/Service Accounts
    'utility', 'prepaid', 'gift_card',

    -- Business Accounts
    'business_checking', 'business_savings', 'business_credit',

    -- Other/Miscellaneous
    'cash', 'escrow', 'trust'
));

-- Ensure lowercase constraint still exists
ALTER TABLE public.t_account
DROP CONSTRAINT IF EXISTS ck_account_type_lowercase;

ALTER TABLE public.t_account
ADD CONSTRAINT ck_account_type_lowercase
CHECK (account_type = lower(account_type));

-- Add index for performance on account_type queries
CREATE INDEX IF NOT EXISTS idx_account_type ON public.t_account(account_type);

-- Add index for account category queries (will be useful for reporting)
-- This will support future category-based filtering
CREATE INDEX IF NOT EXISTS idx_account_active_type ON public.t_account(active_status, account_type)
WHERE active_status = true;