-- Performance index for transaction account lookup (Oracle Tests)
-- Migration: V04__add-transaction-account-lookup-index.sql
-- Purpose: Optimize findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc query
-- Performance Impact: ~11,500ms → ~50-200ms expected improvement

-- ================================
-- TRANSACTION ACCOUNT LOOKUP INDEX
-- ================================

-- This index optimizes the most common transaction query pattern:
-- SELECT * FROM t_transaction
-- WHERE account_name_owner = ? AND active_status = 1
-- ORDER BY transaction_date DESC

-- Index definition:
-- - account_name_owner: Primary filter column (high selectivity)
-- - active_status: Secondary filter (most queries use active_status = 1)
-- - transaction_date DESC: Matches ORDER BY clause for index-only scan

DECLARE
    index_exists NUMBER;
BEGIN
    -- Check if index already exists
    SELECT COUNT(*)
    INTO index_exists
    FROM user_indexes
    WHERE index_name = 'IDX_TRX_ACCT_LOOKUP';

    IF index_exists = 0 THEN
        -- Create index
        EXECUTE IMMEDIATE 'CREATE INDEX IDX_TRX_ACCT_LOOKUP ON t_transaction (account_name_owner, active_status, transaction_date DESC)';
        DBMS_OUTPUT.PUT_LINE('Created index: IDX_TRX_ACCT_LOOKUP');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Index IDX_TRX_ACCT_LOOKUP already exists, skipping');
    END IF;
END;
/

-- Index statistics
-- Expected impact:
-- - Query execution time: 11,500ms → 50-200ms
-- - Eliminates full table scan
-- - Enables index range scan for ORDER BY
-- - Tablespace usage: ~50-100MB (depending on row count)
