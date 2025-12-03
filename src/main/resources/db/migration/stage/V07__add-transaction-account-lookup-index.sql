-- Performance index for transaction account lookup
-- Migration: V07__add-transaction-account-lookup-index.sql
-- Purpose: Optimize findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc query
-- Performance Impact: ~11,500ms → ~50-200ms expected improvement

SET client_min_messages TO WARNING;

-- ================================
-- TRANSACTION ACCOUNT LOOKUP INDEX
-- ================================

-- This index optimizes the most common transaction query pattern:
-- SELECT * FROM t_transaction
-- WHERE account_name_owner = ? AND active_status = true
-- ORDER BY transaction_date DESC

-- Index definition:
-- - account_name_owner: Primary filter column (high selectivity)
-- - active_status: Secondary filter (most queries use active_status = true)
-- - transaction_date DESC: Matches ORDER BY clause for index-only scan

DO $$
BEGIN
    -- Check if index already exists
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'public'
        AND tablename = 't_transaction'
        AND indexname = 'idx_transaction_account_lookup'
    ) THEN
        -- Create index
        CREATE INDEX idx_transaction_account_lookup
        ON t_transaction (account_name_owner, active_status, transaction_date DESC);

        RAISE NOTICE 'Created index: idx_transaction_account_lookup';
    ELSE
        RAISE NOTICE 'Index idx_transaction_account_lookup already exists, skipping';
    END IF;
END
$$;

-- Index statistics
-- Expected impact:
-- - Query execution time: 11,500ms → 50-200ms
-- - Eliminates full table scan
-- - Enables index-only scan for ORDER BY
-- - Disk space: ~50-100MB (depending on row count)
