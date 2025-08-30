#!/bin/bash

# cleanup-orphaned-descriptions.sh
# Script to clean up orphaned active descriptions from today's merge operations
# 2025-08-30

set -e

# Database connection parameters
DB_HOST="debian-dockerserver"
DB_CONTAINER="postgresql-server"
DB_NAME="finance_db"
DB_USER="henninb"
export PGPASSWORD="monday1"

echo "=== Orphaned Description Cleanup Script ==="
echo "Date: $(date)"
echo "Target Database: ${DB_NAME} on ${DB_HOST}/${DB_CONTAINER}"
echo ""

# Function to execute SQL command
execute_sql() {
    local sql="$1"
    local description="$2"
    echo "Executing: $description"
    echo "SQL: $sql"
    ssh "$DB_HOST" "docker exec -e PGPASSWORD=$PGPASSWORD $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c \"$sql\""
    echo ""
}

# Function to check if transactions exist using a description
check_transaction_usage() {
    local description="$1"
    echo "Checking transaction usage for: '$description'"
    local count=$(ssh "$DB_HOST" "docker exec -e PGPASSWORD=$PGPASSWORD $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -t -c \"SELECT COUNT(*) FROM t_transaction WHERE description = '$description'\"" | tr -d ' ')
    echo "Found $count transactions using '$description'"
    return $count
}

echo "=== PHASE 1: Analysis of Current State ==="

# Check current orphaned active descriptions
echo "Current orphaned active descriptions:"
execute_sql "SELECT description_name, date_added, date_updated FROM t_description WHERE active_status = true AND description_name NOT IN (SELECT DISTINCT description FROM t_transaction WHERE description IS NOT NULL) ORDER BY date_added DESC;" "List orphaned active descriptions"

echo "=== PHASE 2: Transaction Updates ==="

echo "Case 1: 'serums good time' → 'serums'"
echo "---------------------------------------"

# Check if any transactions are actually using 'serums good time'
if check_transaction_usage "serums good time"; then
    echo "Found transactions using 'serums good time'. Updating to 'serums'..."
    execute_sql "UPDATE t_transaction SET description = 'serums', date_updated = CURRENT_TIMESTAMP WHERE description = 'serums good time';" "Update 'serums good time' to 'serums'"
else
    echo "No transactions found using 'serums good time' - this description can be safely deactivated."
fi

echo "Case 2: 'the hardware' → 'the hardware store'"
echo "----------------------------------------------"

# Check if any transactions are actually using 'the hardware'
if check_transaction_usage "the hardware"; then
    echo "Found transactions using 'the hardware'. Updating to 'the hardware store'..."
    execute_sql "UPDATE t_transaction SET description = 'the hardware store', date_updated = CURRENT_TIMESTAMP WHERE description = 'the hardware';" "Update 'the hardware' to 'the hardware store'"
else
    echo "No transactions found using 'the hardware' - this description can be safely deactivated."
fi

echo "=== PHASE 3: Deactivate Orphaned Active Descriptions ==="

echo "Deactivating 'serums good time'..."
execute_sql "UPDATE t_description SET active_status = false, date_updated = CURRENT_TIMESTAMP WHERE description_name = 'serums good time';" "Deactivate 'serums good time'"

echo "Deactivating 'the hardware'..."
execute_sql "UPDATE t_description SET active_status = false, date_updated = CURRENT_TIMESTAMP WHERE description_name = 'the hardware';" "Deactivate 'the hardware'"

echo "=== PHASE 4: Verification ==="

echo "Verifying no active orphaned descriptions remain:"
execute_sql "SELECT description_name, active_status, date_updated FROM t_description WHERE active_status = true AND description_name NOT IN (SELECT DISTINCT description FROM t_transaction WHERE description IS NOT NULL) ORDER BY date_updated DESC;" "Check for remaining orphaned active descriptions"

echo "Checking updated transactions:"
execute_sql "SELECT transaction_id, description, date_updated FROM t_transaction WHERE description IN ('serums', 'the hardware store') AND DATE(date_updated) = CURRENT_DATE ORDER BY date_updated DESC;" "Show recently updated transactions"

echo "=== PHASE 5: Summary Statistics ==="

echo "Description table statistics:"
execute_sql "SELECT 'Total descriptions' as metric, COUNT(*) as count FROM t_description UNION ALL SELECT 'Active descriptions' as metric, COUNT(*) as count FROM t_description WHERE active_status = true UNION ALL SELECT 'Inactive descriptions' as metric, COUNT(*) as count FROM t_description WHERE active_status = false;" "Description table stats"

echo "Transaction description statistics:"
execute_sql "SELECT 'Unique transaction descriptions' as metric, COUNT(DISTINCT description) as count FROM t_transaction WHERE description IS NOT NULL;" "Transaction description stats"

echo "Orphaned description check:"
execute_sql "SELECT 'Active orphaned descriptions' as metric, COUNT(*) as count FROM t_description WHERE active_status = true AND description_name NOT IN (SELECT DISTINCT description FROM t_transaction WHERE description IS NOT NULL) UNION ALL SELECT 'Inactive orphaned descriptions' as metric, COUNT(*) as count FROM t_description WHERE active_status = false AND description_name NOT IN (SELECT DISTINCT description FROM t_transaction WHERE description IS NOT NULL);" "Orphaned description final count"

echo ""
echo "=== CLEANUP COMPLETE ==="
echo "Date: $(date)"
echo "Summary:"
echo "- Checked for transactions using orphaned active descriptions"
echo "- Updated any found transactions to use proper descriptions"
echo "- Deactivated orphaned active descriptions: 'serums good time' and 'the hardware'"
echo "- Verified referential integrity maintained"
echo ""
echo "Note: This script addressed the 2 active orphaned descriptions from 2025-08-30."
echo "The 52 inactive orphaned descriptions were left as-is for historical reference."
echo ""
echo "Next steps:"
echo "1. Monitor for new orphaned active descriptions after future merge operations"
echo "2. Consider archiving very old inactive descriptions (1+ years unused)"
echo "3. Update DESCRIPTION_DETAILS.md to reflect the cleanup results"