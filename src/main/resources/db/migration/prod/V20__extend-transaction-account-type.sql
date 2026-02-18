-- V20: Widen account_type CHECK constraint on t_transaction
-- The old ck_account_type only allowed ('debit', 'credit', 'undefined') but
-- the compound FK fk_account_id_account_name_owner copies the account's full type
-- (e.g. 'checking', 'savings') into the transaction row. This must match
-- the ck_account_type on t_account.

ALTER TABLE t_transaction DROP CONSTRAINT IF EXISTS ck_account_type;

ALTER TABLE t_transaction ADD CONSTRAINT ck_transaction_account_type CHECK (account_type IN (
    'credit', 'debit', 'undefined',
    'checking', 'savings', 'credit_card', 'certificate', 'money_market',
    'brokerage', 'retirement_401k', 'retirement_ira', 'retirement_roth', 'pension',
    'hsa', 'fsa', 'medical_savings',
    'mortgage', 'auto_loan', 'student_loan', 'personal_loan', 'line_of_credit',
    'utility', 'prepaid', 'gift_card',
    'business_checking', 'business_savings', 'business_credit',
    'cash', 'escrow', 'trust'
));
