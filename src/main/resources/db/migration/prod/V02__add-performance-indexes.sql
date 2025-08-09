-- Performance indexes for production database (transactional part)
-- Migration: V02__add-performance-indexes.sql
-- Purpose: Add critical indexes to improve query performance for financial data operations
-- Note: Concurrent index creation moved to V03 migration

SET client_min_messages TO WARNING;

-- ================================
-- NON-CONCURRENT INDEXES (TRANSACTIONAL)
-- ================================

-- Note: All indexes will be created with CONCURRENTLY in V03 migration
-- This migration creates the foundation for index tracking