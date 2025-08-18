-- Performance indexes for functional test database
-- Migration: V02__add-performance-indexes.sql
-- Purpose: Add critical indexes to improve query performance for financial data operations
-- Note: This is adapted from prod V02 migration for H2 in-memory database

-- ================================
-- INDEXES FOR FUNCTIONAL TESTS
-- ================================

-- Transaction indexes for performance
CREATE INDEX IF NOT EXISTS idx_transaction_account_name_owner ON func.t_transaction (account_name_owner);
CREATE INDEX IF NOT EXISTS idx_transaction_date_updated ON func.t_transaction (date_updated);
CREATE INDEX IF NOT EXISTS idx_transaction_date_added ON func.t_transaction (date_added);
CREATE INDEX IF NOT EXISTS idx_transaction_transaction_date ON func.t_transaction (transaction_date);

-- Payment indexes for performance
CREATE INDEX IF NOT EXISTS idx_payment_source_account ON func.t_payment (source_account);
CREATE INDEX IF NOT EXISTS idx_payment_destination_account ON func.t_payment (destination_account);
CREATE INDEX IF NOT EXISTS idx_payment_transaction_date ON func.t_payment (transaction_date);

-- Account indexes for performance
CREATE INDEX IF NOT EXISTS idx_account_account_name_owner ON func.t_account (account_name_owner);
CREATE INDEX IF NOT EXISTS idx_account_account_type ON func.t_account (account_type);
CREATE INDEX IF NOT EXISTS idx_account_active_status ON func.t_account (active_status);

-- Category indexes for performance
CREATE INDEX IF NOT EXISTS idx_category_category_name ON func.t_category (category_name);
CREATE INDEX IF NOT EXISTS idx_category_active_status ON func.t_category (active_status);

-- Description indexes for performance
CREATE INDEX IF NOT EXISTS idx_description_description_name ON func.t_description (description_name);
CREATE INDEX IF NOT EXISTS idx_description_active_status ON func.t_description (active_status);

-- Validation amount indexes for performance
CREATE INDEX IF NOT EXISTS idx_validation_amount_account_id ON func.t_validation_amount (account_id);
CREATE INDEX IF NOT EXISTS idx_validation_amount_validation_date ON func.t_validation_amount (validation_date);
CREATE INDEX IF NOT EXISTS idx_validation_amount_transaction_state ON func.t_validation_amount (transaction_state);